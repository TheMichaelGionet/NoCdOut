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
                discrete_markov(x)(y)   = ( (1L<<8).toDouble * markov_state_matrix(x)(y) ).toInt
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
    
    val state_randomness                    = new lfsr8( List[UInt]( r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U ) )
    
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
    
    state   := next_state
    
    // Handle coordinate generation
    
    val coordinate_randomness               = new lfsr16( List( r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U, r.nextInt(2).U ) )
    
    val random_coordinates                  = Reg( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    when( ( counter & 1.U ) === 0.U )
    {
        random_coordinates.x                    := ( random_coordinates.x << 1.U ) | ( random_coordinates.x >> (params.noc_x_size_len-1).U ) ^ coordinate_randomness.io.out
        random_coordinates.y                    := ( random_coordinates.y << 1.U ) | ( random_coordinates.y >> (params.noc_y_size_len-1).U )
    }
    .otherwise
    {
        random_coordinates.x                    := ( random_coordinates.x << 1.U ) | ( random_coordinates.x >> (params.noc_x_size_len-1).U )
        random_coordinates.y                    := ( random_coordinates.y << 1.U ) | ( random_coordinates.y >> (params.noc_y_size_len-1).U ) ^ coordinate_randomness.io.out
    }
    
    val coordinates_are_attackable              = memory.map{ x => x.data_valid && ( x.side =/= my_side.U ) && x.owned }
    
    //val given_memory_attack_these_coordinates   = PriorityMux( (memory.map{ x => x.loc } zip coordinates_are_attackable).map{ case(sel,opt) => sel -> opt } )
    val given_memory_attack_these_coordinates   = PriorityMux( coordinates_are_attackable, memory.map{x => x.loc}, random_coordinates, new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    
    val attackable_coordinates                  = if( memory_depth > 0 ) given_memory_attack_these_coordinates else random_coordinates
    
    val coordinates_are_exploitable             = memory.map{ x => x.data_valid && !x.owned }
    
    //val given_memory_exploit_these_coordinates  = PriorityMux( (memory.map{ x => x.loc } zip coordinates_are_attackable).map{ case(sel,opt) => sel -> opt } )
    val given_memory_exploit_these_coordinates  = PriorityMux( coordinates_are_exploitable, memory.map{x => x.loc}, random_coordinates, new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    
    val exploitable_coordinates                 = if( memory_depth > 0 ) given_memory_exploit_these_coordinates else random_coordinates
    
    val coordinates_around_me                   = Wire( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    coordinates_around_me.x                     := ( my_x_pos - (1<< (log_cloud_diam-1)) ).S( (params.noc_x_size_len+1).W ).asUInt + ( random_coordinates.x & ( (1<<log_cloud_diam)-1 ).U )
    coordinates_around_me.y                     := ( my_y_pos - (1<< (log_cloud_diam-1)) ).S( (params.noc_y_size_len+1).W ).asUInt + ( random_coordinates.y & ( (1<<log_cloud_diam)-1 ).U )
    
    
    
    
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
    
    val ship_is_ally            = io.ship_valid && ( io.ship_it_sees.general_id.side === my_side.U )
    val i_own_ship              = ship_is_ally  && ( io.ship_it_sees.general_id.general_owned === my_general_id.U )
    val is_scout                = io.ship_valid && ( io.ship_it_sees.ship_class === ShipClasses.scout.U )
    
    val learn_from_scout_data   = i_own_ship && is_scout && io.ship_it_sees.scout_data.data_valid
    
    val learn_from_ship_probe   = io.ship_valid && !i_own_ship && !is_scout && probe_all_ships.B
    
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





