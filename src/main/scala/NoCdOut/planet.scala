
package planet

import chisel3._
import chisel3.util.Decoupled

import common._

/*

abstract class Sector( params : GameParameters ) extends Module
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

    
}


class Planet(   resource_production_rate    : Int, 
                max_resources               : Int, 
                max_turret_health           : Int, 
                turret_damage_per_hp        : Int, 
                params                      : GameParameters, 
                general_builders            : List[GeneralBuilder],
                default_team                : Int, // -1 - none, 0 - player, 1 - enemy1
                ) extends Sector( params )
{
    val is_owned        = RegInit( (default_team == -1).B )
    val owner           = RegInit( if( default_team == -1 ) 0.U( params.num_players_len.W ) else default_team.U( params.num_players_len.W ) )
    
    val general_DFAs    = general_builders.map{ x => Module( x() ) }
    
    // IO to control the planet behavior
    
    val ship_it_sees    = Reg( new Ship( params ) )
    val ship_valid      = Reg( Bool() )
    
    val resources       = RegInit( 0.U( params.max_resource_val_len.W ) )
    val limit_resources = RegInit( max_resources.U( params.max_resource_val_len.W ) )
    
    val turret_hp       = Reg( UInt( params.max_turret_hp_len.W ) )
    val limit_turret_hp = Reg( UInt( params.max_turret_hp_len.W ) )
    
    val under_attack    = Reg( Bool() )
    
    val do_build_ship   = Wire( Bool() )
    val which_ship      = Wire( UInt( params.num_ship_classes_len.W ) )
    val how_many_ships  = Wire( UInt( params.max_fleet_hp_len.W ) )
    
    val command_ship    = Wire( Bool() )
    val command_where   = Wire( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    
    val add_turret_hp   = Wire( Bool() )
    
    
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
