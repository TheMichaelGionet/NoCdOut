

package general

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._

class more_general_test extends AnyFreeSpec with Matchers {

    "General General" in {

        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )
        
        val test_this_general   = new GeneralGeneralBuilder( params, 1 )
        {
            //this.markov_state_matrix        = List( List( 1.0, 0.0, 0.0, 0.0 ), List( 0.0, 1.0, 0.0, 0.0 ), List( 0.0, 0.0, 1.0, 0.0 ), List( 0.0, 0.0, 0.0, 1.0 ) )
            //this.markov_state_matrix        = List( List( 0.0, 1.0, 0.0, 0.0 ), List( 0.0, 1.0, 0.0, 0.0 ), List( 0.0, 1.0, 0.0, 0.0 ), List( 0.0, 1.0, 0.0, 0.0 ) )
            //this.markov_state_matrix        = List( List( 0.0, 0.0, 1.0, 0.0 ), List( 0.0, 0.0, 1.0, 0.0 ), List( 0.0, 0.0, 1.0, 0.0 ), List( 0.0, 0.0, 1.0, 0.0 ) )
            //this.markov_state_matrix        = List( List( 0.0, 0.0, 0.0, 1.0 ), List( 0.0, 0.0, 0.0, 1.0 ), List( 0.0, 0.0, 0.0, 1.0 ), List( 0.0, 0.0, 0.0, 1.0 ) )
            //this.markov_state_matrix        = List( List( 0.90, 0.10, 0.0, 0.0 ), List( 0.0, 0.90, 0.10, 0.0 ), List( 0.0, 0.0, 0.90, 0.10 ), List( 0.0, 0.0, 0.0, 1.0 ) )
            this.markov_state_matrix        = List( List( 0.0, 0.5, 0.5, 0.0 ), List( 0.0, 0.5, 0.5, 0.0 ), List( 0.0, 0.5, 0.5, 0.0 ), List( 0.0, 0.5, 0.5, 0.0 ) )
            //this.memory_depth               = 0
            //this.memory_depth               = 1
            this.memory_depth               = 2
            this.my_side                    = 0
            this.my_general_id              = 1
            this.my_x_pos                   = 10
            this.my_y_pos                   = 11
            //this.probe_all_ships            = false
            this.probe_all_ships            = true
            //this.emergency_build_defences   = false
            this.emergency_build_defences   = true
            //this.make_defence_cloud         = false
            this.make_defence_cloud         = true
            //this.log_cloud_diam             = 0
            this.log_cloud_diam             = 2
            //this.ship_build_hp_bias         = 0
            this.ship_build_hp_bias         = 100
            this.prefer_beefy_ships         = false
            //this.prefer_beefy_ships         = true
            this.my_turret_limit            = 50
        }
        
        val markov = MarkovStatesToRange( test_this_general.markov_state_matrix )
        
        println( "Markov ranges:" )
        for( src <- 0 until 4 )
        {
            var le_string = ""
            for( dst <- 0 until 4 )
            {
                le_string += markov(src)(dst) + ", "
            }
            println( le_string )
        }
        
        simulate( test_this_general() ) 
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
            dut.io.ship_it_sees.scout_data.owned.poke( false.B )
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
                // Scout returned with data
                if( cycles == 10 )
                {
                    dut.io.ship_it_sees.src.x.poke( 1.U )
                    dut.io.ship_it_sees.src.y.poke( 2.U )
                    dut.io.ship_it_sees.dst.x.poke( 10.U )
                    dut.io.ship_it_sees.dst.y.poke( 11.U )
                    dut.io.ship_it_sees.general_id.side.poke( 0.U )
                    dut.io.ship_it_sees.general_id.general_owned.poke( 1.U )
                    dut.io.ship_it_sees.ship_class.poke( 0.U )
                    dut.io.ship_it_sees.fleet_hp.poke( 5.U )
                    dut.io.ship_it_sees.scout_data.data_valid.poke( true.B )
                    dut.io.ship_it_sees.scout_data.owned.poke( true.B )
                    dut.io.ship_it_sees.scout_data.loc.x.poke( 1.U )
                    dut.io.ship_it_sees.scout_data.loc.y.poke( 2.U )
                    dut.io.ship_it_sees.scout_data.side.poke( 1.U )
                    dut.io.ship_valid.poke( true.B )
                }
                else if( cycles == 11 )
                {
                    dut.io.ship_it_sees.src.x.poke( 0.U )
                    dut.io.ship_it_sees.src.y.poke( 0.U )
                    dut.io.ship_it_sees.dst.x.poke( 0.U )
                    dut.io.ship_it_sees.dst.y.poke( 0.U )
                    dut.io.ship_it_sees.general_id.side.poke( 0.U )
                    dut.io.ship_it_sees.general_id.general_owned.poke( 0.U )
                    dut.io.ship_it_sees.ship_class.poke( 0.U )
                    dut.io.ship_it_sees.fleet_hp.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.data_valid.poke( false.B )
                    dut.io.ship_it_sees.scout_data.owned.poke( false.B )
                    dut.io.ship_it_sees.scout_data.loc.x.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.loc.y.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.side.poke( 0.U )
                    dut.io.ship_valid.poke( false.B )
                }

                // Ship attacking from another planet
                if( cycles == 50 )
                {
                    dut.io.ship_it_sees.src.x.poke( 5.U )
                    dut.io.ship_it_sees.src.y.poke( 6.U )
                    dut.io.ship_it_sees.dst.x.poke( 10.U )
                    dut.io.ship_it_sees.dst.y.poke( 11.U )
                    dut.io.ship_it_sees.general_id.side.poke( 1.U )
                    dut.io.ship_it_sees.general_id.general_owned.poke( 2.U )
                    dut.io.ship_it_sees.ship_class.poke( 2.U )
                    dut.io.ship_it_sees.fleet_hp.poke( 10.U )
                    dut.io.ship_it_sees.scout_data.data_valid.poke( false.B )
                    dut.io.ship_it_sees.scout_data.owned.poke( false.B )
                    dut.io.ship_it_sees.scout_data.loc.x.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.loc.y.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.side.poke( 0.U )
                    dut.io.ship_valid.poke( true.B )
                    dut.io.under_attack.poke( true.B )
                }
                else if( cycles == 51 )
                {
                    dut.io.ship_it_sees.src.x.poke( 0.U )
                    dut.io.ship_it_sees.src.y.poke( 0.U )
                    dut.io.ship_it_sees.dst.x.poke( 0.U )
                    dut.io.ship_it_sees.dst.y.poke( 0.U )
                    dut.io.ship_it_sees.general_id.side.poke( 0.U )
                    dut.io.ship_it_sees.general_id.general_owned.poke( 0.U )
                    dut.io.ship_it_sees.ship_class.poke( 0.U )
                    dut.io.ship_it_sees.fleet_hp.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.data_valid.poke( false.B )
                    dut.io.ship_it_sees.scout_data.owned.poke( false.B )
                    dut.io.ship_it_sees.scout_data.loc.x.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.loc.y.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.side.poke( 0.U )
                    dut.io.ship_valid.poke( false.B )
                    dut.io.under_attack.poke( false.B )
                }

                // Scout returned with data
                if( cycles == 100 )
                {
                    dut.io.ship_it_sees.src.x.poke( 13.U )
                    dut.io.ship_it_sees.src.y.poke( 3.U )
                    dut.io.ship_it_sees.dst.x.poke( 10.U )
                    dut.io.ship_it_sees.dst.y.poke( 11.U )
                    dut.io.ship_it_sees.general_id.side.poke( 0.U )
                    dut.io.ship_it_sees.general_id.general_owned.poke( 1.U )
                    dut.io.ship_it_sees.ship_class.poke( 0.U )
                    dut.io.ship_it_sees.fleet_hp.poke( 5.U )
                    dut.io.ship_it_sees.scout_data.data_valid.poke( true.B )
                    dut.io.ship_it_sees.scout_data.owned.poke( false.B )
                    dut.io.ship_it_sees.scout_data.loc.x.poke( 13.U )
                    dut.io.ship_it_sees.scout_data.loc.y.poke( 3.U )
                    dut.io.ship_it_sees.scout_data.side.poke( 0.U )
                    dut.io.ship_valid.poke( true.B )
                }
                else if( cycles == 101 )
                {
                    dut.io.ship_it_sees.src.x.poke( 0.U )
                    dut.io.ship_it_sees.src.y.poke( 0.U )
                    dut.io.ship_it_sees.dst.x.poke( 0.U )
                    dut.io.ship_it_sees.dst.y.poke( 0.U )
                    dut.io.ship_it_sees.general_id.side.poke( 0.U )
                    dut.io.ship_it_sees.general_id.general_owned.poke( 0.U )
                    dut.io.ship_it_sees.ship_class.poke( 0.U )
                    dut.io.ship_it_sees.fleet_hp.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.data_valid.poke( false.B )
                    dut.io.ship_it_sees.scout_data.owned.poke( false.B )
                    dut.io.ship_it_sees.scout_data.loc.x.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.loc.y.poke( 0.U )
                    dut.io.ship_it_sees.scout_data.side.poke( 0.U )
                    dut.io.ship_valid.poke( false.B )
                }

                //println( "cycle =             " + cycles )
                //println( "build ship?         " + dut.io.do_build_ship.peekValue().asBigInt )
                //println( "which ship?         " + dut.io.which_ship.peekValue().asBigInt )
                //println( "how many ships?     " + dut.io.how_many_ships.peekValue().asBigInt )
                //println( "command where?      " + dut.io.command_where.x.peekValue().asBigInt + ", " + dut.io.command_where.y.peekValue().asBigInt )
                //println( "build turret?       " + dut.io.add_turret_hp.peekValue().asBigInt )
                //println( "how much turret hp? " + dut.io.how_much_turret_hp.peekValue().asBigInt )
                //println( "" )
                // Step the simulation forward.
                dut.clock.step()
                cycles += 1
            }
        }
    }
}
