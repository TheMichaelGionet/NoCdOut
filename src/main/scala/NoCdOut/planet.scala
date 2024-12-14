
package planet

import chisel3._
import chisel3.util.Decoupled

import common._



abstract class Sector( params : GameParameters, buffer_depth : Int ) extends Module
{
    val io = IO(new Bundle
    {
        val in = new Bundle
        {
            val bp          = Output( Bool() )
            val ship        = Input( new Ship( params ) )
            val ship_valid  = Input( Bool() )
        }
        val out = new Bundle
        {
            val bp          = Input( Bool() )
            val ship        = Output( new Ship( params ) )
            val ship_valid  = Output( Bool() )
        }
    })

    val input_buffer    = Module( new FIFO( buffer_depth, new Ship( params ) ) )
    val output_buffer   = Module( new FIFO( buffer_depth, new Ship( params ) ) )
    
    input_buffer.io.in.valid    := io.in.ship_valid
    input_buffer.io.in.bits     := io.in.ship
    io.in.bp                    := !input_buffer.io.in.ready
    
    io.out.ship_valid           := output_buffer.io.out.valid
    io.out.ship                 := output_buffer.io.out.bits
    output_buffer.io.out.ready  := !io.out.bp
}

class ShipEventHandler( params : GameParameters, x_pos : Int, y_pos : Int ) extends Module
{
    val io = IO( new Bundle
    {
        val ship            = Input( new Ship( params ) )
        val ship_valid      = Input( Bool() )

        val new_ship        = Input( new Ship( params ) )
        val new_ship_valid  = Input( Bool() )
        
        val new_route       = Input( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
        
        val is_owned        = Input( Bool() ) // the planet, that is
        val owner           = Input( new GeneralID( params.num_players_len, params.num_generals_len ) )
        
        val ship_out        = Output( new Ship( params ) )
        val ship_out_valid  = Output( Bool() )
        
        val do_damage       = Output( Bool() )
    } )

    val here = Wire( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    here.x  := x_pos.U
    here.y  := y_pos.U

    val ship_enemy      = !io.is_owned || ( io.ship.general_id.side =/= io.owner.side )
    val i_own_the_ship  = io.ship.general_id.general_owned === io.owner.general_owned && io.ship.general_id.side === io.owner.side

    val is_scout        = io.ship.ship_class    === ShipClasses.scout.U
    
    // default to internal, but prioritize external.
    // Reroute whenever it's friendly, or it's an enemy scout.
    val ship_multiplex  = ( io.ship_valid && ( !ship_enemy || is_scout ) )
    
    io.do_damage        := ship_enemy && !is_scout && io.ship_valid
    
    io.ship_out_valid   := io.new_ship_valid    || ship_multiplex
    
    val multiplexed_ship    = Mux( ship_multiplex, io.ship, io.new_ship )
    
    io.ship_out.general_id      := multiplexed_ship.general_id
    io.ship_out.ship_class      := multiplexed_ship.ship_class
    io.ship_out.fleet_hp        := multiplexed_ship.fleet_hp
    
    val no_data                 = Wire( new ScoutData( params.noc_x_size_len, params.noc_y_size_len, params.num_players_len ) )
    no_data.data_valid          := false.B
    no_data.loc.x               := 0.U
    no_data.loc.y               := 0.U
    no_data.side                := 0.U
    no_data.owned               := false.B

    val this_planet_data        = Wire( new ScoutData( params.noc_x_size_len, params.noc_y_size_len, params.num_players_len ) )
    this_planet_data.data_valid := true.B
    this_planet_data.loc.x      := x_pos.U
    this_planet_data.loc.y      := y_pos.U
    this_planet_data.side       := io.owner.side
    this_planet_data.owned      := io.is_owned
    
    when( ( ship_multiplex && i_own_the_ship ) || !ship_multiplex )
    {
        io.ship_out.src         := here
        io.ship_out.dst         := io.new_route
        
        io.ship_out.scout_data  := no_data
    }
    .elsewhen( ship_multiplex && !ship_enemy && !i_own_the_ship || io.ship_valid && ship_enemy && is_scout )
    {
        io.ship_out.dst         := multiplexed_ship.src
        io.ship_out.src         := multiplexed_ship.dst
        
        io.ship_out.scout_data  := Mux( is_scout, this_planet_data, no_data )
    }
    .otherwise // when( ship_enemy && !is_scout )
    {
        // don't care, no routing. 
        io.ship_out.src         := here
        io.ship_out.dst         := here
        
        io.ship_out.scout_data  := no_data
    }
}

/*
class Planet(   resource_production_rate    : Int, 
                max_resources               : Int, 
                max_turret_health           : Int, 
                turret_damage_per_hp        : Int, 
                params                      : GameParameters, 
                general_builders            : List[GeneralBuilder],
                default_team                : Int, // -1 - none, 0 - player, 1 - enemy1
                buffer_depth                : Int,
                x_pos                       : Int, // the position on the NoC. 
                y_pos                       : Int
                ) extends Sector( params, buffer_depth )
{
    val is_owned        = RegInit( (default_team == -1).B )
    val owner           = RegInit( if( default_team == -1 ) 0.U( params.num_players_len.W ) else default_team.U( params.num_players_len.W ) )
    
    val general_DFAs    = general_builders.map{ x => Module( x() ) }

    val new_ship        = Reg( new Ship( params ) )
    val new_ship_valid  = RegInit( false.B )
    
    // IO to control the planet behavior
    
    val ship_it_sees    = Wire( new Ship( params ) )
    val ship_valid      = Wire( Bool() )
    
    val resources       = RegInit( 0.U( params.max_resource_val_len.W ) )
    val limit_resources = RegInit( max_resources.U( params.max_resource_val_len.W ) )
    
    val turret_hp       = Reg( UInt( params.max_turret_hp_len.W ) )
    val limit_turret_hp = Reg( UInt( params.max_turret_hp_len.W ) )
    
    val under_attack    = Wire( Bool() )
    
    
    val do_build_ship   = Wire( Bool() )
    val which_ship      = Wire( UInt( params.num_ship_classes_len.W ) )
    val how_many_ships  = Wire( UInt( params.max_fleet_hp_len.W ) )
    
    val command_ship    = Wire( Bool() )
    val command_where   = Wire( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    
    val add_turret_hp   = Wire( Bool() )
    
    ship_it_sees        := input_buffer.io.out.bits
    ship_valid          := input_buffer.io.out.valid
    input_buffer_io.out.ready   := true.B // MUST process as it comes up. 
    
    
    // IO to/from each general
    
    val do_build_ship_vec   = Wire( Vec( params.num_players, Bool() ) )
    val which_ship_vec      = Wire( Vec( params.num_players, UInt( params.num_ship_classes_len.W ) ) )
    val how_many_ships_vec  = Wire( Vec( params.num_players, UInt( params.max_fleet_hp_len.W ) ) )
    
    val command_ship_vec    = Wire( Vec( params.num_players, Bool() ) )
    val command_where_vec   = Wire( Vec( params.num_players, new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) ) )
    
    val add_turret_hp_vec   = Wire( Vec( params.num_players, Bool() ) )
    
    for( i <- 0 until params.num_players )
    {
        generals(i).io.ship_it_sees     := ship_it_sees
        generals(i).io.ship_valid       := ship_valid
        generals(i).io.resources        := resources
        generals(i).io.limit_resources  := limit_resources
        generals(i).io.turret_hp        := turret_hp
        generals(i).io.limit_turret_hp  := limit_turret_hp
        generals(i).io.under_attack     := under_attack

        do_build_ship_vec(i)            := generals(i).io.do_build_ship
        which_ship_vec(i)               := generals(i).io.which_ship
        how_many_ships_vec(i)           := generals(i).io.how_many_ships
        command_ship_vec(i)             := generals(i).io.command_ship
        command_where_vec(i)            := generals(i).io.command_where
        add_turret_hp_vec(i)            := generals(i).io.add_turret_hp
    }
    
    // Multiplex based on who has control
    
    when( is_owned )
    {
        
    }
    .otherwise
    {
        do_build_ship                   := false.B
        which_ship                      := 0.U
        how_many_ships                  := 0.U
        
        command_ship                    := true.B
        when(  )
    }
}
*/

