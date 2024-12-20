package playme

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._
import ships._
import general._
import planet._
import levelbuild._
import level_desc._

class playable extends AnyFreeSpec with Matchers {


    "playable" in {
        
        val level_one = new levelOneDescription
        
        val params      = level_one.params
        val descriptors = level_one.descriptors
        
        simulate( new LevelNocBuilder( params, 2, descriptors ) ) 
        {
            dut =>
            
            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            
            var cycle = 0
            
            def get_noc_state_character( x : Int, y : Int ) : String = 
            {
                if( dut.state_observation.has_combat(x)(y).peekValue().asBigInt == 1 )
                {
                    return "#"
                }
                else if( dut.state_observation.more_than_one_ship(x)(y).peekValue().asBigInt == 1 )
                {
                    return "X"
                }
                else if( dut.state_observation.one_ship(x)(y).peekValue().asBigInt == 1 )
                {
                    return "v"
                }
                else
                {
                    return " "
                }
            }
            
            def print_planet_map() = 
            {
                var list_of_other_events    = List[String]()
                for( y <- 0 until params.noc_y_size )
                {
                    var le_string = ""
                    for( x <- 0 until params.noc_x_size )
                    {
                        if( dut.io.meta(x)(y).is_planet.peekValue().asBigInt == 1 )
                        {
                            val planet_index    = dut.io.meta(x)(y).planet_index.peekValue.asBigInt
                            val planet          = dut.state_observation.le_vec(planet_index.toInt)
                            
                            if( ( dut.io.le_vec(x)(y).in.ship_valid.peekValue.asBigInt == 1 ) || ( dut.io.le_vec(x)(y).out.ship_valid.peekValue.asBigInt == 1 ) )
                            {
                                le_string   += "{"
                                if( dut.io.le_vec(x)(y).in.ship_valid.peekValue.asBigInt == 1 )
                                {
                                    list_of_other_events = list_of_other_events    :+ ( "ship entering planet at " + x + ", " + y + " is backpressured? " + dut.io.le_vec(x)(y).in.bp.peekValue.asBigInt.toInt )
                                }
                                if( dut.io.le_vec(x)(y).out.ship_valid.peekValue.asBigInt == 1 )
                                {
                                    list_of_other_events = list_of_other_events    :+ ( "ship leaving planet at " + x + ", " + y + " is backpressured? " + dut.io.le_vec(x)(y).out.bp.peekValue.asBigInt.toInt )
                                }
                            }
                            else
                            {
                                le_string   += "("
                            }
                            
                            if( planet.garrison_valid.peekValue.asBigInt == 1 )
                            {
                                le_string   += "> "
                            }
                            else
                            {
                                val first_digit     = (planet_index & 0xf).toInt.toHexString
                                val second_digit    = ( ( planet_index >> 4 ) & 0xf ).toInt.toHexString
                                le_string           += second_digit
                                le_string           += first_digit
                            }
                        }
                        else
                        {
                            if( dut.io.le_vec(x)(y).in.ship_valid.peekValue.asBigInt == 1 )
                            {
                                le_string   += ">"

                                list_of_other_events = list_of_other_events    :+ ( "ship entering empty space at " + x + ", " + y + " is backpressured? " + dut.io.le_vec(x)(y).in.bp.peekValue.asBigInt.toInt )
                            }
                            else
                            {
                                le_string   += " "
                            }

                            if( dut.io.le_vec(x)(y).out.ship_valid.peekValue.asBigInt == 1 )
                            {
                                le_string   += ".>"
                                list_of_other_events = list_of_other_events    :+ ( "ship exiting empty space at " + x + ", " + y + " is backpressured? " + dut.io.le_vec(x)(y).out.bp.peekValue.asBigInt.toInt )
                            }
                            else
                            {
                                le_string   += ". "
                            }
                        }
                        le_string += get_noc_state_character( x, y )
                    }
                    println( le_string )
                }
                val print_other_events = false
                if( print_other_events )
                {
                    for( ev <- list_of_other_events )
                    {
                        println( ev )
                    }
                }
                println( "" )
                println( "" )
            }
            
            def print_generals() =
            {
                println( "d00d: cycle, is_owned, owned_by, resources, limit_resources, resource_prod, turret_hp, garrison_valid" )
                for( x <-  0 until descriptors.length )
                {
                    println( "general " + x + ": " + cycle + 
                        ", " + dut.state_observation.le_vec(x).is_owned.peekValue().asBigInt +
                        ", " + dut.state_observation.le_vec(x).owned_by.peekValue().asBigInt + 
                        ", " + dut.state_observation.le_vec(x).resources.peekValue().asBigInt +
                        ", " + dut.state_observation.le_vec(x).limit_resources.peekValue().asBigInt +
                        ", " + dut.state_observation.le_vec(x).resource_prod.peekValue().asBigInt +
                        ", " + dut.state_observation.le_vec(x).turret_hp.peekValue().asBigInt +
                        ", " + dut.state_observation.le_vec(x).garrison_valid.peekValue().asBigInt )
                }
            }
            def check_if_done() : Boolean =
            {
                var a_side  = 0
                for( p <- 0 until descriptors.length )
                {
                    if( dut.state_observation.le_vec(p).is_owned.peekValue().asBigInt == 1 )
                    {
                        a_side  = dut.state_observation.le_vec(p).owned_by.peekValue().asBigInt.toInt
                    }
                }

                for( p <- 0 until descriptors.length )
                {
                    if( dut.state_observation.le_vec(p).is_owned.peekValue().asBigInt == 1 )
                    {
                        if( a_side != dut.state_observation.le_vec(p).owned_by.peekValue().asBigInt.toInt )
                        {
                            return false
                        }
                    }
                }
                
                if( a_side == 0 )
                {
                    println( "======================================" )
                    println( "============   You Win!   ============" )
                    println( "======================================" )
                }
                else
                {
                    println( "======================================" )
                    println( "============  You lost :( ============" )
                    println( "======================================" )
                }
                return true
            }
            
            
            

            print_generals()
            print_planet_map()

            dut.clock.step()
            cycle += 1
            
            val cycle_limit = 2000
            
            var game_speed_counter = 0
            
            while( (cycle < cycle_limit) && !check_if_done() )
            {
                print_generals()
                print_planet_map()
                
                dut.clock.step()
                cycle += 1
                if( cycle < 100 )
                {
                    Thread.sleep( 1000 )
                }
                else if( cycle < 500 )
                {
                    Thread.sleep( 250 )
                }
                else if( cycle < 1500 )
                {
                    Thread.sleep( 100 )
                }
                else
                {
                    // Just fast enough to have a coherent render. 
                    Thread.sleep( 15 )
                }
            }
            
            if( cycle == cycle_limit )
            {
                println( "======================================" )
                println( "============ Stalemate :O ============" )
                println( "======================================" )
            }
        }
    }
}

