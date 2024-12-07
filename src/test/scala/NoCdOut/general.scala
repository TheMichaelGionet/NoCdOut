

package general

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._

class general_test extends AnyFreeSpec with Matchers {

    "Jeff" in {

        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )
        
        val buffs = new GeneralJeffBuffs( params )
        
        simulate( new GeneralJeffDFA( params, buffs, 0 ) ) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            //println( "value is " + dut.io.out.peekValue().asBigInt )

            dut.io.ship_it_sees.src.x.poke( 0.U )
            dut.io.ship_it_sees.src.y.poke( 0.U )
            dut.io.ship_it_sees.dst.x.poke( 0.U )
            dut.io.ship_it_sees.dst.y.poke( 0.U )
            dut.io.ship_it_sees.general_id.side.poke( 0.U )
            dut.io.ship_it_sees.general_id.general_owned.poke( 0.U )
            dut.io.ship_it_sees.ship_class.poke( 0.U )
            dut.io.ship_it_sees.fleet_hp.poke( 0.U )
            dut.io.ship_it_sees.scout_data.data_valid.poke( false.B )
            dut.io.ship_it_sees.scout_data.loc.x.poke( 0.U )
            dut.io.ship_it_sees.scout_data.loc.y.poke( 0.U )
            dut.io.ship_it_sees.scout_data.side.poke( 0.U )
            dut.io.ship_valid.poke( false.B )

            dut.io.resources.poke( 60.U )
            dut.io.limit_resources.poke( 100.U )

            dut.io.turret_hp.poke( 10.U )
            dut.io.limit_turret_hp.poke( 40.U )
            
            dut.io.under_attack.poke( false.B )

            var cycles = 0

            while (cycles <= 512) 
            {
                if( cycles == 180 )
                {
                    dut.io.ship_valid.poke(true.B)
                }
                else if( cycles == 181 )
                {
                    dut.io.ship_valid.poke(false.B)
                }

                //println( "cycle =           " + cycles )
                //println( "build ship?       " + dut.io.do_build_ship.peekValue().asBigInt )
                //println( "which ship?       " + dut.io.which_ship.peekValue().asBigInt )
                //println( "how many ships?   " + dut.io.how_many_ships.peekValue().asBigInt )
                //println( "command ship?     " + dut.io.command_ship.peekValue().asBigInt )
                //println( "command where?    " + dut.io.command_where.x.peekValue().asBigInt + ", " + dut.io.command_where.y.peekValue().asBigInt )
                //println( "build turret?     " + dut.io.add_turret_hp.peekValue().asBigInt )
                //println( "" )
                // Step the simulation forward.
                dut.clock.step()
                cycles += 1
            }
        }
    }
}
