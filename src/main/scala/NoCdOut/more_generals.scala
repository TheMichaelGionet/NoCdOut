package general

import chisel3._
import chisel3.util.Decoupled

import scala.util.Random

import common._
import lfsr._

object States
{
    val explore = 0
    val exploit = 1
    val attack  = 2
    val defend  = 3
    val num_states = 4
}

object MarkovStatesToRange
{
    def apply( markov_state_matrix : List[List[Double]] ) : Array[Array[Int]] = 
    {
        val discrete_markov = Array.tabulate(States.num_states,States.num_states)( (_,_) => 0 )
    
        for( x <- 0 until States.num_states )
        {
            for( y <- 0 until States.num_states )
            {
                discrete_markov(x)(y)   = ( ((1L<<8) - 1).toDouble * markov_state_matrix(x)(y) ).toInt
            }
        }
    
        val markov_ranges   = Array.tabulate(States.num_states,States.num_states)( (_,_) => 0 )
    
        for( current_state <- 0 until States.num_states )
        {
            var accum = 0
            for( next_state <- 0 until States.num_states )
            {
                markov_ranges(current_state)(next_state)    = accum
                accum += discrete_markov(current_state)(next_state)
            }
        }
        
        return markov_ranges
    }
}

class GeneralGeneralDFA(    params : GameParameters, 
                            buffs : GeneralBuffs, 
                            markov_state_matrix : List[List[Double]], 
                            memory_depth : Int, 
                            my_side : Int, 
                            my_general_id : Int, 
                            my_x_pos : Int, 
                            my_y_pos : Int, 
                            probe_all_ships : Boolean, 
                            emergency_build_defences : Boolean, 
                            make_defence_cloud : Boolean, 
                            log_cloud_diam : Int, 
                            ship_build_hp_bias : Int, 
                            prefer_beefy_ships : Boolean,
                            my_turret_limit : Int ) extends GeneralDFA( params, buffs )
{
    val r           = new scala.util.Random
    val counter     = RegInit( 0.U(32.W) )
    counter         := counter + 1.U
    
    val markov_ranges   = MarkovStatesToRange( markov_state_matrix )
    val markov_lut      = Reg( Vec( States.num_states, Vec( States.num_states, UInt( 8.W ) ) ) )
    
    val is_first_frame  = RegInit( true.B )
    is_first_frame      := false.B
    
    val memory          = Reg( Vec( memory_depth, new ScoutData( params.noc_x_size, params.noc_y_size, params.num_players_len ) ) )
    
    val state       = RegInit( States.explore.U( bits.needed( States.num_states-1 ).W ) )
    
    when( is_first_frame )
    {
        for( i <- 0 until memory_depth )
        {
            memory(i).data_valid    := false.B
        }
        
        for( x <- 0 until States.num_states )
        {
            for( y <- 0 until States.num_states )
            {
                markov_lut(x)(y)    := markov_ranges(x)(y).U
            }
        }
        state   := States.explore.U
    }
    
    val state_randomness                    = Module( new lfsr8( List[UInt]( r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U ) ) )
    
    val number_to_compare_for_randomness    = RegInit( 7.U(8.W) )
    number_to_compare_for_randomness        := ( number_to_compare_for_randomness << 1 ) | ( number_to_compare_for_randomness >> 7 ) ^ state_randomness.io.out

    val next_state  = Wire( UInt( bits.needed( States.num_states-1 ).W ) )
    
    val relevant_markov_lut_entry   = Wire( Vec( States.num_states, UInt( 8.W ) ) )
    relevant_markov_lut_entry       := markov_lut(state)
    
    val next_state_is_explore   = ( number_to_compare_for_randomness <= relevant_markov_lut_entry(1) )
    val next_state_is_exploit   = ( number_to_compare_for_randomness > relevant_markov_lut_entry(1) ) && ( number_to_compare_for_randomness <= relevant_markov_lut_entry(2) )
    val next_state_is_attack    = ( number_to_compare_for_randomness > relevant_markov_lut_entry(2) ) && ( number_to_compare_for_randomness <= relevant_markov_lut_entry(3) )
    val next_state_is_defend    = ( number_to_compare_for_randomness > relevant_markov_lut_entry(3) )
    
    next_state  := Mux( next_state_is_explore, States.explore.U, 
                        Mux( next_state_is_exploit, States.exploit.U,
                            Mux( next_state_is_attack, States.attack.U, States.defend.U ) 
                            ) 
                        )
    
    state       := next_state

    
    // Used for both coordinate generation and memory handling
    val ship_is_ally            = io.ship_valid && ( io.ship_it_sees.general_id.side === my_side.U )
    val i_own_ship              = ship_is_ally  && ( io.ship_it_sees.general_id.general_owned === my_general_id.U )
    val is_scout                = io.ship_valid && ( io.ship_it_sees.ship_class === ShipClasses.scout.U )
    
    val learn_from_scout_data   = i_own_ship && is_scout && io.ship_it_sees.scout_data.data_valid
    
    val learn_from_ship_probe   = io.ship_valid && !i_own_ship && !is_scout && probe_all_ships.B
    
    // Handle coordinate generation
    
    val coordinate_randomness               = Module( new lfsr16( List( r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U ) ) )
    
    val random_coordinates                  = Reg( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    random_coordinates.x                    := ( random_coordinates.x << 1.U ) | ( random_coordinates.y >> (params.noc_x_size_len-1).U ) ^ coordinate_randomness.io.out
    random_coordinates.y                    := ( random_coordinates.y << 1.U ) | ( random_coordinates.x >> (params.noc_y_size_len-1).U )
    //when( ( counter & 1.U ) === 0.U )
    //{
    //    random_coordinates.x                    := ( random_coordinates.x << 1.U ) | ( random_coordinates.x >> (params.noc_x_size_len-1).U ) ^ coordinate_randomness.io.out
    //    random_coordinates.y                    := ( random_coordinates.y << 1.U ) | ( random_coordinates.y >> (params.noc_y_size_len-1).U )
    //}
    //.otherwise
    //{
    //    random_coordinates.x                    := ( random_coordinates.x << 1.U ) | ( random_coordinates.x >> (params.noc_x_size_len-1).U )
    //    random_coordinates.y                    := ( random_coordinates.y << 1.U ) | ( random_coordinates.y >> (params.noc_y_size_len-1).U ) ^ coordinate_randomness.io.out
    //}
    
    val coordinates_are_attackable              = memory.map{ x => x.data_valid && ( x.side =/= my_side.U ) && x.owned }
    
    //val given_memory_attack_these_coordinates   = PriorityMux( (memory.map{ x => x.loc } zip coordinates_are_attackable).map{ case(sel,opt) => sel -> opt } )
    val given_memory_attack_these_coordinates   = PriorityMux( coordinates_are_attackable, memory.map{x => x.loc}, random_coordinates, new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    
    val attack_coordinates_not_imminent         = if( memory_depth > 0 ) given_memory_attack_these_coordinates else random_coordinates
    
    val attack_scout_coordinates                = learn_from_scout_data && ( io.ship_it_sees.scout_data.side =/= my_side.U ) && io.ship_it_sees.scout_data.owned
    
    val counter_attack_probe_ship               = learn_from_ship_probe && ( io.ship_it_sees.general_id.side =/= my_side.U )
    
    val attackable_coordinates                  = Mux( attack_scout_coordinates, io.ship_it_sees.scout_data.loc,
                                                    Mux( counter_attack_probe_ship, io.ship_it_sees.src, attack_coordinates_not_imminent ) )
    
    val coordinates_are_exploitable             = memory.map{ x => x.data_valid && !x.owned }
    
    //val given_memory_exploit_these_coordinates  = PriorityMux( (memory.map{ x => x.loc } zip coordinates_are_attackable).map{ case(sel,opt) => sel -> opt } )
    val given_memory_exploit_these_coordinates  = PriorityMux( coordinates_are_exploitable, memory.map{x => x.loc}, random_coordinates, new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    
    val exploit_scout_coordinates               = learn_from_scout_data && !io.ship_it_sees.scout_data.owned
    
    val exploitable_coordinates_not_imminent    = if( memory_depth > 0 ) given_memory_exploit_these_coordinates else random_coordinates
    
    val exploitable_coordinates                 = Mux( exploit_scout_coordinates, io.ship_it_sees.scout_data.loc, exploitable_coordinates_not_imminent )
    
    val coordinates_around_me                   = Wire( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    coordinates_around_me.x                     := ( (my_x_pos - (1<< (log_cloud_diam-1))) % params.noc_x_size ).S( (params.noc_x_size_len+1).W ).asUInt + ( random_coordinates.x & ( (1<<log_cloud_diam)-1 ).U )
    coordinates_around_me.y                     := ( (my_y_pos - (1<< (log_cloud_diam-1))) % params.noc_y_size ).S( (params.noc_y_size_len+1).W ).asUInt + ( random_coordinates.y & ( (1<<log_cloud_diam)-1 ).U )

    
    
    // Handle economy decisions/command ship

    io.do_build_ship        := (( state === States.explore.U ) || 
                                ( state === States.exploit.U ) || 
                                ( state === States.attack.U ) || 
                                ( Mux( make_defence_cloud.B, state === States.defend.U, false.B ) ) ) &&
                                ( Mux( emergency_build_defences.B, io.resources > 30.U, true.B ) )
    
    io.which_ship           :=      Mux( state === States.explore.U, ShipClasses.scout.U, 
                                    Mux( state === States.exploit.U, (if(prefer_beefy_ships) ShipClasses.beefer else ShipClasses.basic).U,
                                    Mux( state === States.attack.U,  (if(prefer_beefy_ships) ShipClasses.destroyer else ShipClasses.attack).U,
                                                                    ShipClasses.defence.U ) ) )
    
    io.how_many_ships       := ship_build_hp_bias.U 
    
    io.add_turret_hp        := ( ( state === States.defend.U ) && ( io.turret_hp < my_turret_limit.U ) ) || ( io.under_attack && emergency_build_defences.B )
    io.how_much_turret_hp   := 15.U
    
    io.command_where        :=  Mux( state === States.explore.U, random_coordinates, 
                                Mux( state === States.exploit.U, exploitable_coordinates,
                                Mux( state === States.attack.U,  attackable_coordinates,
                                                                coordinates_around_me ) ) )
    
    // Handle memory stuff
    
    if( memory_depth > 0 )
    {
        when( learn_from_scout_data )
        {
            for( i <- 1 until memory_depth )
            {
                memory( i ) := memory( i-1 )
            }
            memory(0)       := io.ship_it_sees.scout_data
        }
        .elsewhen( learn_from_ship_probe )
        {
            for( i <- 1 until memory_depth )
            {
                memory( i ) := memory( i-1 )
            }
            memory(0).data_valid    := true.B
            memory(0).loc           := io.ship_it_sees.src
            memory(0).owned         := true.B
            memory(0).side          := io.ship_it_sees.general_id.side
        }
    }
}


class GeneralGeneralBuilder( params : GameParameters, general_id : Int ) extends GeneralBuilder( params, general_id )
{
    buffs       = new GeneralNoneBuffs( params )
    var markov_state_matrix : List[List[Double]] = List( List( 1.0, 0.0, 0.0, 0.0 ), List( 0.0, 1.0, 0.0, 0.0 ), List( 0.0, 0.0, 1.0, 0.0 ), List( 0.0, 0.0, 0.0, 1.0 ) )
    
    var memory_depth                = 0
    var my_side                     = 0
    var my_general_id               = general_id
    var my_x_pos                    = 0
    var my_y_pos                    = 0
    var probe_all_ships             = false
    var emergency_build_defences    = false
    var make_defence_cloud          = false
    var log_cloud_diam              = 0
    var ship_build_hp_bias          = 0
    var prefer_beefy_ships          = false
    var my_turret_limit             = 50
    
    def gen()   = new GeneralGeneralDFA( params, 
                            buffs, 
                            markov_state_matrix, 
                            memory_depth, 
                            my_side, 
                            my_general_id, 
                            my_x_pos, 
                            my_y_pos, 
                            probe_all_ships, 
                            emergency_build_defences, 
                            make_defence_cloud, 
                            log_cloud_diam, 
                            ship_build_hp_bias, 
                            prefer_beefy_ships,
                            my_turret_limit )
}


class Timur( params : GameParameters, buffs : GeneralBuffs ) extends GeneralDFA( params, buffs )
{
    val r           = new scala.util.Random
    val first_frame = RegInit( true.B )
    first_frame     := false.B

    object TimurStates
    {
        val snot                = 0
        val get_sick            = 1
        val buy_heaters         = 2
        val build_slum          = 3
        val become_enlightened  = 4
        val yell_at_video_games = 5
        val count               = 6
    }
    
    object TimurStateTimers
    {
        val snot                = 10
        val get_sick            = 20
        val buy_heaters         = 5
        val build_slum          = 10
        val become_enlightened  = 1
        val yell_at_video_games = 5
    }
    
    val next_state_lut              = RegInit( VecInit( List( TimurStates.get_sick.U, TimurStates.buy_heaters.U, TimurStates.build_slum.U, TimurStates.become_enlightened.U, TimurStates.yell_at_video_games.U, TimurStates.snot.U ) ) )
    val next_timer_lut              = RegInit( VecInit( List( TimurStateTimers.get_sick.U, TimurStateTimers.buy_heaters.U, TimurStateTimers.build_slum.U, TimurStateTimers.become_enlightened.U, TimurStateTimers.yell_at_video_games.U, TimurStateTimers.snot.U ) ) )
    
    val state                       = RegInit( TimurStates.snot.U( bits.needed( TimurStates.count-1 ).W ) )
    val next_state                  = next_state_lut( state )
    
    val state_switch_counter        = RegInit( TimurStateTimers.snot.U )
    val next_state_switch_counter   = next_timer_lut( state )
    
    when( state_switch_counter > 1.U )
    {
        state_switch_counter    := state_switch_counter - 1.U
    }
    .otherwise
    {
        state_switch_counter    := next_state_switch_counter
        state                   := next_state
    }
    
    val stochasticity       = Module( new lfsr32( List( r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, 
                                                        r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U,
                                                        r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, 
                                                        r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U ) ) )
    
    val random_x            = RegInit( r.nextInt( params.noc_x_size ).U( params.noc_x_size_len.W ) )
    val random_y            = RegInit( r.nextInt( params.noc_y_size ).U( params.noc_y_size_len.W ) )
    
    random_x                := (random_x << 1) | ( random_y >> ( params.noc_y_size_len-1 ) ) ^ stochasticity.io.out
    random_y                := (random_y << 1) | ( random_x >> ( params.noc_y_size_len-1 ) )
    
    val random_coords       = Wire( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    
    random_coords.x         := random_x
    random_coords.y         := random_y
    
    val memory              = Reg( new ScoutData( params.noc_x_size, params.noc_y_size, params.num_players_len ) )
    
    val learn               = io.ship_valid && ( io.ship_it_sees.ship_class === ShipClasses.scout.U ) && io.ship_it_sees.scout_data.data_valid
    
    when( learn )
    {
        memory              := io.ship_it_sees.scout_data
    }
    
    // friend, foe, or empty planet. No one is safe from the beast. 
    val MAD                 = memory.data_valid && ( state === TimurStates.yell_at_video_games.U )
    
    when( first_frame )
    {
        memory.data_valid   := false.B
    }
    
    io.do_build_ship        := ( state =/= TimurStates.get_sick.U ) && ( state =/= TimurStates.build_slum.U )
    
    io.add_turret_hp        := ( state === TimurStates.build_slum.U )
    
    io.how_much_turret_hp   := 5.U
    
    when( MAD )
    {
        io.command_where    := memory.loc
        io.how_many_ships   := 100.U
    }
    .otherwise
    {
        io.command_where    := random_coords
        io.how_many_ships   := 5.U
    }
    
    when(       state === TimurStates.snot.U )
    {
        io.which_ship       := ShipClasses.basic.U
    }
    .elsewhen(  state === TimurStates.buy_heaters.U )
    {
        io.which_ship       := ShipClasses.destroyer.U
    }
    .elsewhen(  state === TimurStates.become_enlightened.U )
    {
        io.which_ship       := ShipClasses.scout.U
    }
    .elsewhen(  state === TimurStates.yell_at_video_games.U )
    {
        io.which_ship       := ShipClasses.attack.U
    }
    .otherwise // should never execute, but need the default case regardless
    {
        io.which_ship       := ShipClasses.beefer.U
    }
    
    
}

class TimurBuilder( params : GameParameters, general_id : Int ) extends GeneralBuilder( params, general_id )
{
    buffs       = new GeneralNoneBuffs( params )
    def gen()   = new Timur( params, buffs )
}

class OobleckBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.8, 0.15, 0.0, 0.05 ), List( 0.5, 0.3, 0.0, 0.2 ), List( 0.1, 0.2, 0.6, 0.1 ), List( 0.1, 0.0, 0.1, 0.8 ) )
    
    memory_depth                = 4
    probe_all_ships             = true
    emergency_build_defences    = true
    ship_build_hp_bias          = 20
    my_turret_limit             = 100
}

class BushManMikeBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.6, 0.1, 0.1, 0.2 ), List( 0.0, 0.8, 0.0, 0.2 ), List( 0.1, 0.0, 0.6, 0.3 ), List( 0.0, 0.0, 0.1, 0.9 ) )
    
    memory_depth                = 1
    ship_build_hp_bias          = 40
    my_turret_limit             = 50
    emergency_build_defences    = true
    make_defence_cloud          = true
    log_cloud_diam              = 2
}

class SpaceManMikeBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.5, 0.4, 0.0, 0.1 ), List( 0.2, 0.8, 0.0, 0.0 ), List( 0.1, 0.1, 0.5, 0.3 ), List( 0.0, 0.0, 0.1, 0.9 ) )
    
    memory_depth                = 2
    ship_build_hp_bias          = 30
    my_turret_limit             = 75
    make_defence_cloud          = true
    log_cloud_diam              = 5
    probe_all_ships             = true
}

class DaBorgBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.5, 0.1, 0.3, 0.1 ), List( 0.2, 0.6, 0.2, 0.0 ), List( 0.1, 0.0, 0.9, 0.0 ), List( 0.0, 0.1, 0.8, 0.1 ) )
    
    memory_depth                = 4
    ship_build_hp_bias          = 5
    my_turret_limit             = 100
    probe_all_ships             = true
}

class WilliamBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.1, 0.0, 0.0, 0.9 ), List( 0.1, 0.1, 0.8, 0.0 ), List( 0.1, 0.1, 0.0, 0.8 ), List( 0.1, 0.1, 0.0, 0.8 ) )
    
    memory_depth                = 1
    ship_build_hp_bias          = 100
    my_turret_limit             = 400
    prefer_beefy_ships          = true
    probe_all_ships             = true
}

class OnionBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.1, 0.8, 0.0, 0.1 ), List( 0.2, 0.8, 0.0, 0.0 ), List( 0.0, 0.3, 0.6, 0.1 ), List( 0.0, 0.0, 0.2, 0.8 ) )
    
    memory_depth                = 1
    ship_build_hp_bias          = 100
    my_turret_limit             = 20
    make_defence_cloud          = true
    log_cloud_diam              = 4
}

class GregBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.1, 0.1, 0.8, 0.0 ), List( 0.0, 0.2, 0.8, 0.0 ), List( 0.0, 0.1, 0.8, 0.1 ), List( 0.1, 0.0, 0.8, 0.1 ) )
    
    memory_depth                = 1
    ship_build_hp_bias          = 5
    my_turret_limit             = 80
    emergency_build_defences    = true
}

class SkippyBuilder( params : GameParameters, general_id : Int ) extends GeneralGeneralBuilder( params, general_id )
{
    markov_state_matrix         = List( List( 0.1, 0.0, 0.0, 0.9 ), List( 0.0, 0.2, 0.0, 0.8 ), List( 0.0, 0.0, 0.0, 1.0 ), List( 0.1, 0.1, 0.0, 0.8 ) )
    
    memory_depth                = 1
    ship_build_hp_bias          = 20
    my_turret_limit             = 40
    make_defence_cloud          = true
    log_cloud_diam              = 5
}



