
package common

import chisel3._
import chisel3.util.Decoupled

import scala.math.log

object bits
{
    def needed( max_val : Int ) : Int = 
    {
        var acc     = 0
        var temp    = max_val
        
        while( temp > 0 )
        {
            acc += 1
            temp = temp >> 1
        }
        return acc
    }
}

class GameParametersInput
{
    var num_players         = 0x2;
    var num_generals        = 0x5;
    var noc_x_size          = 0x40;
    var noc_y_size          = 0x40;
    var num_ship_classes    = 0x5;
    var max_fleet_hp        = 0xfff;
    var max_turret_hp       = 0xfff;
    var max_resource_val    = 0xffff;
}

class GameParameters( params : GameParametersInput )
{
    val num_players             = params.num_players
    val num_players_len         = bits.needed( params.num_players ) // no -1. "none" should be an extra option.
    
    val num_generals            = params.num_generals
    val num_generals_len        = bits.needed( params.num_generals-1 )
    
    val noc_x_size              = params.noc_x_size
    val noc_x_size_len          = bits.needed( params.noc_x_size-1 )

    val noc_y_size              = params.noc_y_size
    val noc_y_size_len          = bits.needed( params.noc_y_size-1 )

    val num_ship_classes        = params.num_ship_classes
    val num_ship_classes_len    = bits.needed( params.num_ship_classes-1 )

    val max_fleet_hp            = params.max_fleet_hp
    val max_fleet_hp_len        = bits.needed( params.max_fleet_hp ) // no -1

    val max_turret_hp           = params.max_turret_hp
    val max_turret_hp_len       = bits.needed( params.max_turret_hp ) // no -1

    val max_resource_val        = params.max_resource_val
    val max_resource_val_len    = bits.needed( params.max_resource_val ) // no -1
}

class GeneralID( num_players_len : Int, general_len : Int ) extends Bundle
{
    val side            = UInt( num_players_len.W )
    val general_owned   = UInt( general_len.W )
}

object GeneralID
{
    def apply( params : GameParameters ) : GeneralID = 
    {
        return new GeneralID( params.num_players_len, params.num_generals_len )
    }
}

class Coordinates( x_len : Int, y_len : Int ) extends Bundle
{
    val x = UInt( x_len.W )
    val y = UInt( y_len.W )
}

class ScoutData( x_len : Int, y_len : Int, num_players_len : Int ) extends Bundle
{
    val data_valid  = Bool()
    val loc         = new Coordinates( x_len, y_len )
    val owned       = Bool()
    val side        = UInt( num_players_len.W )
}

//class Ship( x_len : Int, y_len : Int, num_players_len : Int, general_len : Int, num_ship_classes_len : Int, max_fleet_hp_len ) extends Bundle
class Ship( params : GameParameters ) extends Bundle
{
    val src             = Output(new Coordinates( params.noc_x_size_len, params.noc_y_size_len ))
    val dst             = Output(new Coordinates( params.noc_x_size_len, params.noc_y_size_len ))
    val general_id      = Output(new GeneralID( params.num_players_len, params.num_generals_len ))
    val ship_class      = Output(UInt( params.num_ship_classes_len.W ))
    val fleet_hp        = Output(UInt( params.max_fleet_hp_len.W ))
    val scout_data      = Output(new ScoutData( params.noc_x_size_len, params.noc_y_size_len, params.num_players_len ))
    //val valid           = UInt(1.W) //CHANGING TO DECOUPLED
    //val backpressured   = UInt(1.W)
}

object ShipClasses
{
    val scout       = 0
    val basic       = 1
    val attack      = 2
    val defence     = 3
    val beefer      = 4
    val destroyer   = 5
    val count       = 6
}

// FIFO implemented as a ring buffer
class FIFO[T <: chisel3.Data]( n : Int, dt : T ) extends Module
{
    val io = IO( new Bundle
    {
        val in  = Flipped( Decoupled( dt ) )
        val out = Decoupled( dt )
    })
    
    val storage         = Reg( Vec( n, dt ) )
    
    val write_ptr       = RegInit( 0.U( bits.needed(n-1).W ) )
    val read_ptr        = RegInit( 0.U( bits.needed(n-1).W ) )
    val full            = RegInit( false.B )
    
    val next_write_ptr  = Wire( UInt( bits.needed(n-1).W ) )
    val next_read_ptr   = Wire( UInt( bits.needed(n-1).W ) )
    val is_full_next    = Wire( Bool() )
    
    next_write_ptr      := Mux( io.in.fire,     Mux( write_ptr  === (n-1).U, 0.U, write_ptr+1.U ),  write_ptr )
    next_read_ptr       := Mux( io.out.fire,    Mux( read_ptr   === (n-1).U, 0.U, read_ptr+1.U ),   read_ptr )
    
    // The two cases are "write catches up to read" and "write and read advance, and was previously full"
    is_full_next        := ( ( write_ptr =/= next_write_ptr ) || full ) && ( next_read_ptr === next_write_ptr )
    
    io.out.valid        := ( write_ptr =/= read_ptr ) || full
    io.in.ready         := !full
    
    write_ptr           := next_write_ptr
    read_ptr            := next_read_ptr
    full                := is_full_next
    
    when( io.in.fire )
    {
        storage( write_ptr )    := io.in.bits
    }
    
    io.out.bits := storage( read_ptr )
}

class PriorityMux[T <: chisel3.Data]( n : Int, dt : T ) extends Module
{
    val io = IO( new Bundle
    {
        val selects = Input( Vec( n, Bool() ) )
        val ins     = Input( Vec( n, dt ) )
        val default = Input( dt )
        
        val out     = Output( dt )
    } )
    
    val partials    = Wire( Vec( n+1, dt ) )
    
    for( i <- 0 until n )
    {
        partials(i) := Mux( io.selects(i), io.ins(i), partials(i+1) )
    }
    partials(n)     := io.default
    
    io.out          := partials(0)
}

object PriorityMux
{
    def apply[T <: chisel3.Data]( selects : Seq[Bool], ins : Seq[T], default : T, dt : T ) : T =
    {
        val pmux        = Module( new PriorityMux( selects.length, dt ) )
        pmux.io.default := default
        
        for( i <- 0 until selects.length )
        {
            pmux.io.selects(i)  := selects(i)
            pmux.io.ins(i)      := ins(i)
        }
        return pmux.io.out
    }
}

