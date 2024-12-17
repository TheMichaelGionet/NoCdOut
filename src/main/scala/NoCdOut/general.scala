
package general

import chisel3._
import chisel3.util.Decoupled

import common._
import lfsr._

// The buffs may or may not get implemented depending on time.
abstract class GeneralBuffs( params : GameParameters )
{
    def resource_prod_buff  : Double
    def combat_buff         : Double
    def cost_buff           : Double
    def routing_behavior    : Int // Which VC to use
    
}

abstract class GeneralDFA( params : GameParameters, val buffs : GeneralBuffs ) extends Module
{
    val io = IO( new Bundle
    {
        val ship_it_sees        = Input( new Ship( params ) )
        val ship_valid          = Input( Bool() )
        
        val resources           = Input( UInt( params.max_resource_val_len.W ) )
        val limit_resources     = Input( UInt( params.max_resource_val_len.W ) )

        val turret_hp           = Input( UInt( params.max_turret_hp_len.W ) )
        val limit_turret_hp     = Input( UInt( params.max_turret_hp_len.W ) )

        val under_attack        = Input( Bool() )
        val ship_was_built      = Input( Bool() )

        val do_build_ship       = Output( Bool() )
        val which_ship          = Output( UInt( params.num_ship_classes_len.W ) )
        val how_many_ships      = Output( UInt( params.max_fleet_hp_len.W ) )
        
        val command_where       = Output( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
        
        val add_turret_hp       = Output( Bool() )
        val how_much_turret_hp  = Output( UInt( params.max_turret_hp_len.W ) )
    } )
}

class GeneralNoneBuffs( params : GameParameters ) extends GeneralBuffs( params )
{
    def resource_prod_buff  = 1.0
    def combat_buff         = 1.0
    def cost_buff           = 1.0
    def routing_behavior    = 0
}

class GeneralTestDFA( params : GameParameters, buffs : GeneralBuffs, value_it_spit_out : Int ) extends GeneralDFA( params, buffs )
{
    io.do_build_ship        := false.B
    io.which_ship           := 0.U
    io.how_many_ships       := value_it_spit_out.U
    io.command_where.x      := 0.U
    io.command_where.y      := 0.U
    io.add_turret_hp        := 0.U
    io.how_much_turret_hp   := 0.U
}

class GeneralJeffBuffs( params : GameParameters ) extends GeneralBuffs( params )
{
    def resource_prod_buff  = 1.0
    def combat_buff         = 1.0
    def cost_buff           = 1.0
    def routing_behavior    = 0
}

// Jeff is a bare bones general
class GeneralJeffDFA( params : GameParameters, buffs : GeneralBuffs, general_id : Int ) extends GeneralDFA( params, buffs )
{
    val counter = RegInit( 0.U(16.W) )
    counter := Mux( counter === 0xff.U, 0xff.U, counter+1.U)

    val startup = Wire(Bool()) 
    startup :=  counter < 0xff.U

    val add_turret_early    = ( ( counter & 0x7.U ) === 0.U )
    val add_turret_later    = io.under_attack

    io.add_turret_hp        := Mux( startup, add_turret_early, add_turret_later )
    io.how_much_turret_hp   := 10.U
    
    val do_build_ship_early = ( ( counter & 0x7.U ) === 3.U )
    val do_build_ship_later = io.resources > ( io.limit_resources >> 1 )
    
    io.do_build_ship    := Mux( startup, do_build_ship_early, do_build_ship_later )
    io.which_ship       := Mux( startup, ShipClasses.scout.U, ShipClasses.basic.U )
    
    val LFSR            = Module( new lfsr16( List[UInt]( 0.U, 1.U, 0.U, 1.U, 0.U, 1.U, 0.U, 1.U, 0.U, 1.U, 0.U, 1.U, 0.U, 1.U, 0.U, 1.U ) ) )
    
    val x_loc           = RegInit( 0x3e.U( params.noc_x_size_len.W ) )
    val y_loc           = RegInit( 0x1f.U( params.noc_y_size_len.W ) )
    
    x_loc := x_loc << 1 | ( LFSR.io.out ^ x_loc( params.noc_x_size_len-1 ) )
    y_loc := y_loc << 1 | ( 1.U ^ LFSR.io.out ^ y_loc( params.noc_y_size_len-1 ) )

    io.how_many_ships   := Mux( startup, 1.U, 5.U )

    io.command_where.x  := x_loc // send ships randomly
    io.command_where.y  := y_loc
}

abstract class GeneralBuilder( params : GameParameters, general_id : Int )
{
    var buffs       : GeneralBuffs  = new GeneralNoneBuffs( params )
    def gen()       : GeneralDFA
    def apply()     : GeneralDFA    = gen()
}

class GeneralJeffBuilder( params : GameParameters, general_id : Int ) extends GeneralBuilder( params, general_id )
{
    buffs       = new GeneralJeffBuffs( params )
    def gen()   = new GeneralJeffDFA( params, buffs, general_id )
}

class GeneralTestBuilder( params : GameParameters, le_parameter_to_test_and_make_sure_it_all_otherwise_works_but_isnt_for_real_applications : Int, general_id : Int ) extends GeneralBuilder( params, general_id )
{
    buffs       = new GeneralNoneBuffs( params )
    def gen()   = new GeneralTestDFA( params, buffs, le_parameter_to_test_and_make_sure_it_all_otherwise_works_but_isnt_for_real_applications )
}


