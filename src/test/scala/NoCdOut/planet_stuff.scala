

package planet

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._

class planet_test extends AnyFreeSpec with Matchers {

    "ship event handler" in {
        
        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )

        
        simulate( new ShipEventHandler( params, 1, 2 ) ) 
        {
            dut =>

            /*
            val ship            = Input( new Ship( params ) )
            val ship_valid      = Input( Bool() )

            val new_ship        = Input( new Ship( params ) )
            val new_ship_valid  = Input( Bool() )
        
            val new_route       = Input( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
        
            val is_owned        = Input( Bool() ) // the planet, that is
            val owner           = Input( new GeneralID( params.num_players_len, params.general_len ) )
        
            val ship_out        = Output( new Ship( params ) )
            val ship_out_valid  = Output( Bool() )
        
            val do_damage       = Output( Bool() )
            */

            dut.io.ship.src.x.poke( 3.U )
            dut.io.ship.src.y.poke( 4.U )
            dut.io.ship.dst.x.poke( 1.U )
            dut.io.ship.dst.y.poke( 2.U )
            dut.io.ship.general_id.side.poke( 0.U )
            dut.io.ship.general_id.general_owned.poke( 0.U )
            dut.io.ship.ship_class.poke( 0.U )
            dut.io.ship.fleet_hp.poke( 0.U )
            dut.io.ship.scout_data.data_valid.poke( false.B )
            dut.io.ship.scout_data.loc.x.poke( 0.U )
            dut.io.ship.scout_data.loc.y.poke( 0.U )
            dut.io.ship.scout_data.side.poke( 0.U )

            dut.io.ship_valid.poke( false.B )
            dut.io.new_route.x.poke( 5.U )
            dut.io.new_route.y.poke( 6.U )
            dut.io.is_owned.poke( false.B )
            dut.io.owner.side.poke( 0.U )
            dut.io.owner.general_owned.poke( 1.U )

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            //println( "value is " + dut.io.out.peekValue().asBigInt )
            
            dut.io.do_damage.expect( false.B )
            dut.io.ship_out_valid.expect( false.B )
            
            // ----------- case unowned
            // if unowned and a valid ship is routed to the planet, then it takes damage (iff not scout)

            dut.io.ship_valid.poke(true.B)
            dut.io.do_damage.expect(false.B) // It's set to a scout
            //println( "1 ship_out_valid is " + dut.io.ship_out_valid.peekValue().asBigInt )
            dut.io.ship_out_valid.expect( true.B )

            //println( "2 ship_out.scout_data.data_valid is " + dut.io.ship_out.scout_data.data_valid.peekValue().asBigInt )
            dut.io.ship_out.scout_data.data_valid.expect(true.B)
            dut.io.ship_out.scout_data.loc.x.expect(1.U)
            dut.io.ship_out.scout_data.loc.y.expect(2.U)
            //dut.io.ship_out.scout_data.side.expect(0.U)
            dut.io.ship_out.scout_data.owned.expect(false.B)

            dut.io.ship.ship_class.poke(1.U)
            //println( "3 do_damage is " + dut.io.do_damage.peekValue().asBigInt )
            dut.io.do_damage.expect(true.B) // It's set to basic
            dut.io.ship_out_valid.expect( false.B )
            dut.io.ship_out.scout_data.data_valid.expect(false.B)

            dut.io.ship_valid.poke(false.B)
            dut.io.ship.ship_class.poke(0.U)

            // ----------- case owned
            dut.io.is_owned.poke( true.B )
            // if owned, default do nothing
            
            dut.io.ship_out_valid.expect( false.B )
            dut.io.do_damage.expect( false.B )
            
            // if ship is valid and I own it, it gets routed to the new location specified
            dut.io.ship_valid.poke(true.B)
            dut.io.ship.general_id.side.poke( 0.U )
            dut.io.ship.general_id.general_owned.poke( 1.U )
            
            // case scout:
            //println( "4 ship_out_valid is " + dut.io.ship_out_valid.peekValue().asBigInt )
            dut.io.ship.ship_class.poke(0.U)
            dut.io.do_damage.expect(false.B)
            dut.io.ship_out_valid.expect( true.B )
            dut.io.ship_out.scout_data.data_valid.expect(false.B) // wipe data
            dut.io.ship_out.src.x.expect( 1.U )
            dut.io.ship_out.src.y.expect( 2.U )
            dut.io.ship_out.dst.x.expect( 5.U )
            dut.io.ship_out.dst.y.expect( 6.U )

            // case other:
            dut.io.ship.ship_class.poke(1.U)
            //println( "5 ship_out_valid is " + dut.io.ship_out_valid.peekValue().asBigInt )
            dut.io.do_damage.expect(false.B)
            dut.io.ship_out_valid.expect( true.B )
            dut.io.ship_out.scout_data.data_valid.expect(false.B)
            dut.io.ship_out.src.x.expect( 1.U )
            dut.io.ship_out.src.y.expect( 2.U )
            dut.io.ship_out.dst.x.expect( 5.U )
            dut.io.ship_out.dst.y.expect( 6.U )

            // if ship is valid and it's friendly but not owned by me, it gets routed back to it's source

            dut.io.ship.general_id.general_owned.poke( 2.U ) // not me

            dut.io.ship.ship_class.poke(0.U)
            dut.io.do_damage.expect(false.B)
            dut.io.ship_out_valid.expect( true.B )
            dut.io.ship_out.scout_data.data_valid.expect(true.B) 
            dut.io.ship_out.src.x.expect( 1.U )
            dut.io.ship_out.src.y.expect( 2.U )
            dut.io.ship_out.dst.x.expect( 3.U )
            dut.io.ship_out.dst.y.expect( 4.U )

            dut.io.ship_out.scout_data.loc.x.expect(1.U)
            dut.io.ship_out.scout_data.loc.y.expect(2.U)
            dut.io.ship_out.scout_data.side.expect(0.U)
            dut.io.ship_out.scout_data.owned.expect(true.B)

            // case other:
            dut.io.ship.ship_class.poke(1.U)
            dut.io.do_damage.expect(false.B)
            dut.io.ship_out_valid.expect( true.B )
            dut.io.ship_out.scout_data.data_valid.expect(false.B)
            dut.io.ship_out.src.x.expect( 1.U )
            dut.io.ship_out.src.y.expect( 2.U )
            dut.io.ship_out.dst.x.expect( 3.U )
            dut.io.ship_out.dst.y.expect( 4.U )

            dut.io.ship.general_id.general_owned.poke( 1.U ) // dc
            dut.io.ship.general_id.side.poke( 1.U )         // not me
            // if ship is valid and it's an enemy ship and it's a scout, it gets routed back to it's source.
            // in addition, it's scout data gets updated to include whatever it needs. 
            
            dut.io.ship.ship_class.poke(0.U)
            dut.io.do_damage.expect(false.B)
            dut.io.ship_out_valid.expect( true.B )
            dut.io.ship_out.scout_data.data_valid.expect(true.B) 
            dut.io.ship_out.src.x.expect( 1.U )
            dut.io.ship_out.src.y.expect( 2.U )
            dut.io.ship_out.dst.x.expect( 3.U )
            dut.io.ship_out.dst.y.expect( 4.U )

            dut.io.ship_out.scout_data.loc.x.expect(1.U)
            dut.io.ship_out.scout_data.loc.y.expect(2.U)
            dut.io.ship_out.scout_data.side.expect(0.U)
            dut.io.ship_out.scout_data.owned.expect(true.B)

            // case other:
            dut.io.ship.ship_class.poke(1.U)
            dut.io.do_damage.expect(true.B)
            dut.io.ship_out_valid.expect( false.B )
            
            
        }
    }
}
