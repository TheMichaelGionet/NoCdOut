
package lfsr

import chisel3._
import chisel3.util.Decoupled

class lfsr4( initial_state : List[UInt] ) extends Module
{
    val io = IO(new Bundle
    {
        val out = UInt(1.W)
    })
    
    val state = RegInit( VecInit( initial_state ) )
    
    val next_top    = Wire( UInt(1.W) )
    
    next_top        := state(0) ^ state(1)
    
    for( i <- 0 until 4 )
    {
        if( i == 3 )
        {
            state(i)    := next_top
        }
        else
        {
            state(i)    := state(i+1)
        }
    }

    io.out  := state(0)
}

class lfsr8( initial_state : List[UInt] ) extends Module
{
    val io = IO(new Bundle
    {
        val out = UInt(1.W)
    })
    
    val state = RegInit( VecInit( initial_state ) )
    
    val next_top    = Wire( UInt(1.W) )
    
    next_top        := state(0) ^ state(2) ^ state(3) ^ state(4)
    
    for( i <- 0 until 8 )
    {
        if( i == 7 )
        {
            state(i)    := next_top
        }
        else
        {
            state(i)    := state(i+1)
        }
    }

    io.out  := state(0)
}

class lfsr16( initial_state : List[UInt] ) extends Module
{
    val io = IO(new Bundle
    {
        val out = UInt(1.W)
    })
    
    val state = RegInit( VecInit( initial_state ) )
    
    val next_top    = Wire( UInt(1.W) )
    
    next_top        := state(0) ^ state(11) ^ state(13) ^ state(14)
    
    for( i <- 0 until 16 )
    {
        if( i == 15 )
        {
            state(i)    := next_top
        }
        else
        {
            state(i)    := state(i+1)
        }
    }

    io.out  := state(0)
}

class lfsr32( initial_state : List[UInt] ) extends Module
{
    val io = IO(new Bundle
    {
        val out = UInt(1.W)
    })
    
    val state = RegInit( VecInit( initial_state ) )
    
    val next_top    = Wire( UInt(1.W) )
    
    next_top        := state(0) ^ state(1) ^ state(2) ^ state(3) ^ state(5) ^ state(7)
    
    for( i <- 0 until 32 )
    {
        if( i == 31 )
        {
            state(i)    := next_top
        }
        else
        {
            state(i)    := state(i+1)
        }
    }

    io.out  := state(0)
}
