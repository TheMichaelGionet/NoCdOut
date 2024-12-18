

package planet

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._
import general._

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
            val consume_new_ship    = Output( Bool() )
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

            dut.io.new_ship.src.x.poke( 0.U )
            dut.io.new_ship.src.y.poke( 0.U )
            dut.io.new_ship.dst.x.poke( 0.U )
            dut.io.new_ship.dst.y.poke( 0.U )
            dut.io.new_ship.general_id.side.poke( 0.U )
            dut.io.new_ship.general_id.general_owned.poke( 0.U )
            dut.io.new_ship.ship_class.poke( 0.U )
            dut.io.new_ship.fleet_hp.poke( 0.U )
            dut.io.new_ship.scout_data.data_valid.poke( false.B )
            dut.io.new_ship.scout_data.loc.x.poke( 0.U )
            dut.io.new_ship.scout_data.loc.y.poke( 0.U )
            dut.io.new_ship.scout_data.side.poke( 0.U )

            dut.io.new_ship_valid.poke( false.B )

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
            dut.io.consume_new_ship.expect( false.B )
            
            // ----------- case unowned
            // if unowned and a valid ship is routed to the planet, then it takes damage (iff not scout)

            dut.io.ship_valid.poke(true.B)
            dut.io.do_damage.expect(false.B) // It's set to a scout
            //println( "1 ship_out_valid is " + dut.io.ship_out_valid.peekValue().asBigInt )
            dut.io.ship_out_valid.expect( true.B )
            dut.io.consume_new_ship.expect( false.B )

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
            dut.io.consume_new_ship.expect( false.B )

            dut.io.ship_valid.poke(false.B)
            dut.io.ship.ship_class.poke(0.U)

            // ----------- case owned
            dut.io.is_owned.poke( true.B )
            // if owned, default do nothing
            
            dut.io.ship_out_valid.expect( false.B )
            dut.io.do_damage.expect( false.B )
            dut.io.consume_new_ship.expect( false.B )
            
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
            dut.io.consume_new_ship.expect( false.B )

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
            dut.io.consume_new_ship.expect( false.B )

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
            dut.io.consume_new_ship.expect( false.B )

            // case other:
            dut.io.ship.ship_class.poke(1.U)
            dut.io.do_damage.expect(true.B)
            dut.io.ship_out_valid.expect( false.B )
            dut.io.consume_new_ship.expect( false.B )
            
            // case nothing is coming in, but the newly created ship wants to go out:
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

            dut.io.new_ship.src.x.poke( 0.U )
            dut.io.new_ship.src.y.poke( 0.U )
            dut.io.new_ship.dst.x.poke( 0.U )
            dut.io.new_ship.dst.y.poke( 0.U )
            dut.io.new_ship.general_id.side.poke( 0.U )
            dut.io.new_ship.general_id.general_owned.poke( 1.U )
            dut.io.new_ship.ship_class.poke( 2.U )
            dut.io.new_ship.fleet_hp.poke( 5.U )
            dut.io.new_ship.scout_data.data_valid.poke( false.B )
            dut.io.new_ship.scout_data.loc.x.poke( 0.U )
            dut.io.new_ship.scout_data.loc.y.poke( 0.U )
            dut.io.new_ship.scout_data.side.poke( 0.U )

            dut.io.new_ship_valid.poke( true.B )

            dut.io.ship_out_valid.expect( true.B )
            dut.io.ship_out.scout_data.data_valid.expect(false.B) 
            dut.io.ship_out.src.x.expect( 1.U )
            dut.io.ship_out.src.y.expect( 2.U )
            dut.io.ship_out.dst.x.expect( 5.U )
            dut.io.ship_out.dst.y.expect( 6.U )
            dut.io.ship_out.general_id.side.expect( 0.U )
            dut.io.ship_out.general_id.general_owned.expect( 1.U )
            dut.io.ship_out.ship_class.expect( 2.U )
            dut.io.ship_out.fleet_hp.expect( 5.U )
            
            dut.io.consume_new_ship.expect( true.B )

            // case something is coming in, but the newly created ship wants to go out:

            dut.io.ship.src.x.poke( 3.U )
            dut.io.ship.src.y.poke( 4.U )
            dut.io.ship.dst.x.poke( 1.U )
            dut.io.ship.dst.y.poke( 2.U )
            dut.io.ship.general_id.side.poke( 0.U )
            dut.io.ship.general_id.general_owned.poke( 2.U )
            dut.io.ship.ship_class.poke( 1.U )
            dut.io.ship.fleet_hp.poke( 10.U )
            dut.io.ship.scout_data.data_valid.poke( false.B )
            dut.io.ship.scout_data.loc.x.poke( 0.U )
            dut.io.ship.scout_data.loc.y.poke( 0.U )
            dut.io.ship.scout_data.side.poke( 0.U )

            dut.io.ship_valid.poke( true.B )

            dut.io.ship_out_valid.expect( true.B )
            dut.io.ship_out.scout_data.data_valid.expect(false.B) 
            dut.io.ship_out.src.x.expect( 1.U )
            dut.io.ship_out.src.y.expect( 2.U )
            dut.io.ship_out.dst.x.expect( 3.U )
            dut.io.ship_out.dst.y.expect( 4.U )
            dut.io.ship_out.general_id.side.expect( 0.U )
            dut.io.ship_out.general_id.general_owned.expect( 2.U )
            dut.io.ship_out.ship_class.expect( 1.U )
            dut.io.ship_out.fleet_hp.expect( 10.U )
            
            dut.io.consume_new_ship.expect( false.B )
        }
    }

    "combat handler" in {
        
        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )

        
        simulate( new CombatHandler( params ) ) 
        {
            dut =>

            /*
            val ship            = Input( new Ship( params ) )
            
            val do_damage       = Input( Bool() )
            
            val current_hp      = Input( UInt( params.max_turret_hp_len.W ) )
            val max_hp          = Input( UInt( params.max_turret_hp_len.W ) )
            
            val hp_inc          = Input( UInt( params.max_turret_hp_len.W ) )
            
            val new_hp          = Output( UInt( params.max_turret_hp_len.W ) )
            
            val planet_takeover = Output( Bool() )
            
            val new_side        = Output( UInt( num_players_len.W ) )
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

            dut.io.do_damage.poke( false.B )
            dut.io.current_hp.poke( 0.U )
            dut.io.max_hp.poke( 0.U )
            dut.io.hp_inc.poke( 0.U )

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            
            // If nothing happens, nothing happens.
            dut.io.new_hp.expect( 0.U )
            dut.io.planet_takeover.expect(false.B)
            
            // New hp should just be the sum of the old hp + replenishment
            dut.io.current_hp.poke( 10.U )
            dut.io.hp_inc.poke( 5.U )
            dut.io.max_hp.poke( 20.U )
            dut.io.new_hp.expect( 15.U )
            dut.io.planet_takeover.expect(false.B)

            // If rectified from above:
            dut.io.max_hp.poke( 12.U )
            dut.io.new_hp.expect( 12.U )
            dut.io.planet_takeover.expect(false.B)
            
            // Doing damage does damage
            dut.io.hp_inc.poke( 0.U )
            dut.io.do_damage.poke( true.B )
            dut.io.ship.fleet_hp.poke( 1.U )
            dut.io.ship.ship_class.poke( 1.U ) // basic ship
            
            dut.io.new_hp.expect( 8.U ) // 10 - (1<<2) = 8
            dut.io.planet_takeover.expect(false.B)
            
            // Wiping out the hp makes the planet take over.
            dut.io.ship.fleet_hp.poke( 15.U )
            dut.io.current_hp.poke( 5.U )
            dut.io.new_hp.expect( 0.U ) // rectified from below
            dut.io.planet_takeover.expect(true.B)

            // This can be stopped however if it regains enough HP to not die

            dut.io.hp_inc.poke( 30.U )
            dut.io.new_hp.expect( 5.U ) // 5 + 5 - ( 15<<1 ) = 10 - 7 = 3
            dut.io.planet_takeover.expect(false.B)
        }
    }

    "econ handler" in {
        
        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )

        
        simulate( new EconomyHandler( params ) ) 
        {
            dut =>
            
            /*
                val do_build_ship       = Input( Bool() )
                val which_ship          = Input( UInt( params.num_ship_classes_len.W ) )
                val how_many_ships      = Input( UInt( params.max_fleet_hp_len.W ) )
                val add_turret_hp       = Input( Bool() )
                
                val turret_hp_amount    = Input( UInt( params.max_turret_hp_len.W ) )
                
                val general_id          = Input( GeneralID( params ) )
                
                val resources           = Input( UInt( params.max_resource_val_len.W ) )
                val max_resources       = Input( UInt( params.max_resource_val_len.W ) )
                val add_resources       = Input( UInt( params.max_resource_val_len.W ) )
                
                val ship                = Output( new Ship( params ) )
                val ship_valid          = Output( Bool() )
                
                val inc_turret_hp_out   = Output( UInt( params.max_turret_hp_len.W ) )
                
                val resources_after_purchases   = Output( UInt( params.max_resource_val_len.W ) )
            */

            dut.io.do_build_ship.poke( false.B )
            dut.io.which_ship.poke( 0.U )
            dut.io.how_many_ships.poke( 0.U )
            dut.io.add_turret_hp.poke( false.B )
            dut.io.turret_hp_amount.poke( 0.U )
            dut.io.general_id.side.poke( 1.U )
            dut.io.general_id.general_owned.poke( 3.U )
            dut.io.resources.poke( 0.U )
            dut.io.max_resources.poke( 1.U )
            dut.io.add_resources.poke( 0.U )

            dut.io.ship_backpressure.poke( false.B )

            dut.reset.poke( true.B )
            dut.clock.step()
            dut.reset.poke( false.B )

            // Doing nothing does nothing
            dut.io.ship_valid.expect( false.B )
            dut.io.inc_turret_hp_out.expect( 0.U )
            dut.io.resources_after_purchases.expect( 0.U )
            
            // No purchase is just "add resources"
            
            dut.io.resources.poke( 12.U )
            dut.io.max_resources.poke( 20.U )
            dut.io.add_resources.poke( 5.U )
            
            dut.io.resources_after_purchases.expect( 17.U )

            dut.io.max_resources.poke( 15.U )
            dut.io.resources_after_purchases.expect( 15.U )

            // Purchasing a ship succeeds if resources are available, and fails if not.

            dut.io.resources.poke( 30.U )
            dut.io.max_resources.poke( 40.U )
            dut.io.add_resources.poke( 0.U )

            dut.io.do_build_ship.poke( true.B )
            dut.io.which_ship.poke( 1.U ) // purchase basic
            dut.io.how_many_ships.poke( 5.U )

            dut.io.ship_valid.expect( true.B )
            dut.io.resources_after_purchases.expect( 25.U )
            dut.io.inc_turret_hp_out.expect( 0.U )
            dut.io.ship.ship_class.expect( 1.U )
            dut.io.ship.fleet_hp.expect( 5.U )
            dut.io.ship.general_id.side.expect( 1.U )
            dut.io.ship.general_id.general_owned.expect( 3.U )

            dut.io.ship_backpressure.poke( true.B ) // do nothing if backpressured
            dut.io.ship_valid.expect( false.B )


            dut.io.ship_backpressure.poke( false.B )

            // trying to buy too little amount of ships rectifies it to the min amount

            dut.io.how_many_ships.poke( 4.U )

            dut.io.ship_valid.expect( true.B )
            dut.io.resources_after_purchases.expect( 25.U )
            dut.io.inc_turret_hp_out.expect( 0.U )
            dut.io.ship.ship_class.expect( 1.U )
            dut.io.ship.fleet_hp.expect( 5.U )
            dut.io.ship.general_id.side.expect( 1.U )
            dut.io.ship.general_id.general_owned.expect( 3.U )

            // trying to buy too many ships rectifies it to the max amount

            dut.io.how_many_ships.poke( 26.U )

            dut.io.ship_valid.expect( true.B )
            dut.io.resources_after_purchases.expect( 5.U )
            dut.io.inc_turret_hp_out.expect( 0.U )
            dut.io.ship.ship_class.expect( 1.U )
            dut.io.ship.fleet_hp.expect( 25.U )
            dut.io.ship.general_id.side.expect( 1.U )
            dut.io.ship.general_id.general_owned.expect( 3.U )

            // buying a ship and incrementing resources takes that increment into account in the total

            dut.io.add_resources.poke( 5.U )
            dut.io.resources_after_purchases.expect( 10.U )

            // trying to buy a ship with insufficient funds does not work.

            dut.io.add_resources.poke( 0.U )
            dut.io.resources.poke( 20.U )

            dut.io.ship_valid.expect( false.B )
            dut.io.resources_after_purchases.expect( 20.U )

            // Now we just want to buy some turret hp only
            
            dut.io.do_build_ship.poke( false.B )
            dut.io.resources.poke( 30.U )
            dut.io.ship_valid.expect( false.B )
            dut.io.resources_after_purchases.expect( 30.U )

            dut.io.add_turret_hp.poke( true.B )
            dut.io.turret_hp_amount.poke( 5.U )

            dut.io.inc_turret_hp_out.expect( 5.U )
            dut.io.resources_after_purchases.expect( 25.U )
            
            // test that rectification does work
            
            dut.io.turret_hp_amount.poke( 4.U )

            dut.io.inc_turret_hp_out.expect( 5.U )
            dut.io.resources_after_purchases.expect( 25.U )

            dut.io.turret_hp_amount.poke( 16.U )

            dut.io.inc_turret_hp_out.expect( 15.U )
            dut.io.resources_after_purchases.expect( 15.U )

            // Cannot purchase with insufficient funds

            dut.io.resources.poke( 10.U )

            dut.io.inc_turret_hp_out.expect( 0.U )
            dut.io.resources_after_purchases.expect( 10.U )

            // Now we want to purchase both a ship and some turret hp

            dut.io.resources.poke( 50.U )
            dut.io.max_resources.poke( 60.U )
            
            dut.io.do_build_ship.poke( true.B )
            dut.io.which_ship.poke( 2.U ) // purchase attack
            dut.io.how_many_ships.poke( 5.U ) // = 20$
            
            dut.io.turret_hp_amount.poke( 10.U )
            
            dut.io.ship_valid.expect( true.B )
            dut.io.inc_turret_hp_out.expect( 10.U )
            dut.io.ship.ship_class.expect( 2.U )
            dut.io.ship.fleet_hp.expect( 5.U )

            dut.io.resources_after_purchases.expect( 20.U )

            // If there is enough resources for a ship but not for the turret, then the ship succeeds but the turret fails.

            dut.io.resources.poke( 25.U )
            
            dut.io.ship_valid.expect( true.B )
            dut.io.inc_turret_hp_out.expect( 0.U )
            dut.io.ship.ship_class.expect( 2.U )
            dut.io.ship.fleet_hp.expect( 5.U )

            dut.io.resources_after_purchases.expect( 5.U )

            // If there is not enough resources for either, then both fail

            dut.io.resources.poke( 15.U )
            
            dut.io.ship_valid.expect( false.B )
            dut.io.inc_turret_hp_out.expect( 10.U )

            dut.io.resources_after_purchases.expect( 5.U )

            

            dut.io.resources.poke( 5.U )
            
            dut.io.ship_valid.expect( false.B )
            dut.io.inc_turret_hp_out.expect( 0.U )

            dut.io.resources_after_purchases.expect( 5.U )
        }
    }

    "general mux" in {
        
        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params              = new GameParameters( input_params )

        val general0_bill_dor   = new GeneralTestBuilder( params, 69, 1 )
        val general1_bill_dor   = new GeneralTestBuilder( params, 42, 2 ) // Just imagine that there is a 0 after 42
        
        val bill_dorr_list      = List[GeneralBuilder]( general0_bill_dor, general1_bill_dor )
        val general_ids         = List[Int]( 1, 2 )
        
        simulate( new GeneralMux( params, bill_dorr_list, general_ids ) ) 
        {
            dut =>

            dut.reset.poke( true.B )
            dut.clock.step()
            dut.reset.poke( false.B )

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
            
            dut.io.is_owned.poke( false.B )
            dut.io.owner.poke( 0.U )
            dut.io.owner_changed.poke( false.B )
            
            // When not owned, it should just spit out 0
            dut.io.how_many_ships.expect( 0.U )

            // When owned, it spits out whatever from whatever general is selected
            dut.io.is_owned.poke( true.B )
            dut.io.owner.poke( 0.U )

            dut.io.how_many_ships.expect( 69.U )


            dut.io.is_owned.poke( true.B )
            dut.io.owner.poke( 1.U )

            dut.io.how_many_ships.expect( 42.U )
        }
    }


    "empty space should swap src and dst and otherwise keep everything else the same" in {
        
        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )

        

        
        simulate( new EmptySpace( params, 2 ) ) 
        {
            dut =>
            
            dut.io.in.ship_valid.poke( false.B )
            dut.io.out.bp.poke( false.B )

            dut.io.in.ship.src.x.poke( 3.U )
            dut.io.in.ship.src.y.poke( 4.U )
            dut.io.in.ship.dst.x.poke( 1.U )
            dut.io.in.ship.dst.y.poke( 2.U )
            dut.io.in.ship.general_id.side.poke( 1.U )
            dut.io.in.ship.general_id.general_owned.poke( 3.U )
            dut.io.in.ship.ship_class.poke( 2.U )
            dut.io.in.ship.fleet_hp.poke( 5.U )
            dut.io.in.ship.scout_data.data_valid.poke( true.B )
            dut.io.in.ship.scout_data.loc.x.poke( 6.U )
            dut.io.in.ship.scout_data.loc.y.poke( 7.U )
            dut.io.in.ship.scout_data.side.poke( 2.U )

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)

            dut.io.in.ship_valid.poke( true.B )
            
            dut.clock.step()
            dut.io.in.ship_valid.poke( false.B )
            
            var count_cycles = 1
            var did_terminate = false
            //println( "4 ship_out_valid is " + dut.io.ship_out_valid.peekValue().asBigInt )
            while( count_cycles < 10 )
            {
                if( dut.io.out.ship_valid.peekValue.asBigInt == 1 )
                {
                    dut.io.out.ship.dst.x.expect( 3.U )
                    dut.io.out.ship.dst.y.expect( 4.U )
                    dut.io.out.ship.src.x.expect( 1.U )
                    dut.io.out.ship.src.y.expect( 2.U )
                    dut.io.out.ship.general_id.side.expect( 1.U )
                    dut.io.out.ship.general_id.general_owned.expect( 3.U )
                    dut.io.out.ship.ship_class.expect( 2.U )
                    dut.io.out.ship.fleet_hp.expect( 5.U )
                    dut.io.out.ship.scout_data.data_valid.expect( true.B )
                    dut.io.out.ship.scout_data.loc.x.expect( 6.U )
                    dut.io.out.ship.scout_data.loc.y.expect( 7.U )
                    dut.io.out.ship.scout_data.side.expect( 2.U )
                    did_terminate   = true
                }
                dut.clock.step()
                count_cycles += 1
            }
            
            if( did_terminate == false )
            {
                dut.io.out.ship_valid.expect( true.B )
            }
        }
    }



    "test planet" in {
        
        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )

        val bill_dorr0 = new GeneralTestPlanetBuilder( params, 10, 1 )
        val bill_dorr1 = new GeneralTestPlanetBuilder( params, 15, 2 )
        
        val general_builders    = List[GeneralBuilder]( bill_dorr0, bill_dorr1 )
        val general_ids         = List[Int]( 1, 2 )
        
        def planet_build() : Planet =
        {
            val resource_production_rate    = 5
            val max_resources               = 100
            val default_team                = 0
            val owned_by_default            = false
            val buffer_depth                = 2
            val x_pos                       = 1
            val y_pos                       = 3

            return new Planet(  resource_production_rate, 
                                max_resources, 
                                params, 
                                general_builders,
                                general_ids,
                                default_team,
                                owned_by_default,
                                buffer_depth,
                                x_pos, // the position on the NoC. 
                                y_pos)
        }
        
        simulate( planet_build() ) 
        {
            dut =>
            
            dut.io.in.ship_valid.poke( false.B )
            dut.io.out.bp.poke( false.B )

            dut.io.in.ship.src.x.poke( 3.U )
            dut.io.in.ship.src.y.poke( 4.U )
            dut.io.in.ship.dst.x.poke( 1.U )
            dut.io.in.ship.dst.y.poke( 2.U )
            dut.io.in.ship.general_id.side.poke( 0.U )
            dut.io.in.ship.general_id.general_owned.poke( 3.U )
            dut.io.in.ship.ship_class.poke( 2.U )
            dut.io.in.ship.fleet_hp.poke( 5.U )
            dut.io.in.ship.scout_data.data_valid.poke( true.B )
            dut.io.in.ship.scout_data.loc.x.poke( 6.U )
            dut.io.in.ship.scout_data.loc.y.poke( 7.U )
            dut.io.in.ship.scout_data.side.poke( 2.U )

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)

            dut.io.out.ship_valid.expect( false.B )

            dut.state_observation.is_owned.expect( false.B )
            dut.state_observation.resources.expect( 0.U )
            dut.state_observation.limit_resources.expect( 100.U )
            dut.state_observation.resource_prod.expect( 5.U )
            dut.state_observation.turret_hp.expect( 0.U )
            dut.state_observation.garrison_valid.expect( false.B )

            //println( "cycle, is_owned, owned_by, resources, limit_resources, resource_prod, turret_hp, garrison_valid" )

            var cycle = 0

            /*
            println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
            */

            //println(  "" + cycle + 
            //            ", dbg inc turret amnt " + dut.debug_observables.econ_inc_turret_hp_out.peekValue().asBigInt + 
            //            ", dbg combat turret hp out " + dut.debug_observables.combat_turret_hp_out.peekValue().asBigInt + 
            //            ", dbg general_wants_hp " + dut.debug_observables.general_wants_hp.peekValue().asBigInt +
            //            ", dbg ship_is_seen " + dut.debug_observables.ship_is_seen.peekValue().asBigInt +
            //            ", dbg max_hp_seen_by_combat" + dut.debug_observables.ship_is_seen.peekValue().asBigInt )

            dut.io.in.ship_valid.poke( true.B )
            dut.clock.step()
            cycle += 1

            /*
            println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
            */
            
            //println(  "" + cycle + 
            //            ", dbg inc turret amnt " + dut.debug_observables.econ_inc_turret_hp_out.peekValue().asBigInt + 
            //            ", dbg combat turret hp out " + dut.debug_observables.combat_turret_hp_out.peekValue().asBigInt + 
            //            ", dbg general_wants_hp " + dut.debug_observables.general_wants_hp.peekValue().asBigInt +
            //            ", dbg ship_is_seen " + dut.debug_observables.ship_is_seen.peekValue().asBigInt +
            //            ", dbg max_hp_seen_by_combat " + dut.debug_observables.max_hp_seen_by_combat.peekValue().asBigInt )

            dut.io.in.ship_valid.poke( false.B )

            dut.clock.step()
            cycle += 1

            /*
            println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
            */

            dut.clock.step()
                cycle += 1

            //println(  "" + cycle + 
            //            ", dbg inc turret amnt " + dut.debug_observables.econ_inc_turret_hp_out.peekValue().asBigInt + 
            //            ", dbg combat turret hp out " + dut.debug_observables.combat_turret_hp_out.peekValue().asBigInt + 
            //            ", dbg general_wants_hp " + dut.debug_observables.general_wants_hp.peekValue().asBigInt +
            //            ", dbg ship_is_seen " + dut.debug_observables.ship_is_seen.peekValue().asBigInt +
            //            ", dbg max_hp_seen_by_combat " + dut.debug_observables.max_hp_seen_by_combat.peekValue().asBigInt )
            
            dut.state_observation.is_owned.expect( true.B )
            dut.state_observation.owned_by.expect( 0.U )

            while( cycle < 30 )
            {
                /*
                println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
                */

                if( dut.io.out.ship_valid.peekValue().asBigInt == 1 )
                {
                    //println( "ship out at cycle " + cycle )
                    //dut.io.out.ship_valid.expect( true.B )
                    dut.io.out.ship.src.x.expect( 1.U )
                    dut.io.out.ship.src.y.expect( 3.U )
                    dut.io.out.ship.dst.x.expect( 3.U )
                    dut.io.out.ship.dst.y.expect( 4.U )
                    dut.io.out.ship.general_id.side.expect( 0.U )
                    dut.io.out.ship.general_id.general_owned.expect( 1.U )
                    dut.io.out.ship.fleet_hp.expect( 10.U )
                    dut.io.out.ship.scout_data.data_valid.expect( false.B )
                }

                //println(  "" + cycle + 
                //        ", dbg inc turret amnt " + dut.debug_observables.econ_inc_turret_hp_out.peekValue().asBigInt + 
                //        ", dbg combat turret hp out " + dut.debug_observables.combat_turret_hp_out.peekValue().asBigInt + 
                //        ", dbg general_wants_hp " + dut.debug_observables.general_wants_hp.peekValue().asBigInt +
                //        ", dbg ship_is_seen " + dut.debug_observables.ship_is_seen.peekValue().asBigInt +
                //        ", dbg max_hp_seen_by_combat " + dut.debug_observables.max_hp_seen_by_combat.peekValue().asBigInt )

                dut.clock.step()
                cycle += 1
            }
            
            dut.io.in.ship.general_id.side.poke( 1.U )
            dut.io.in.ship_valid.poke( true.B )
            
            /*
            println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
            */
            
            dut.clock.step()
            cycle += 1
            dut.io.in.ship_valid.poke( false.B )
            
            /*
            println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
            */
            
            dut.clock.step()
            cycle += 1
            
            dut.state_observation.owned_by.expect( 1.U )
            
            /*
            println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
            */

            while( cycle < 60 )
            {
                
                /*
                println(  "" + cycle + 
                    ", " + dut.state_observation.is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.resources.peekValue().asBigInt +
                    ", " + dut.state_observation.limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.garrison_valid.peekValue().asBigInt )
                */

                if( dut.io.out.ship_valid.peekValue().asBigInt == 1 )
                {
                    //println( "ship out at cycle " + cycle )
                    //dut.io.out.ship_valid.expect( true.B )
                    dut.io.out.ship.src.x.expect( 1.U )
                    dut.io.out.ship.src.y.expect( 3.U )
                    dut.io.out.ship.dst.x.expect( 3.U )
                    dut.io.out.ship.dst.y.expect( 4.U )
                    dut.io.out.ship.general_id.side.expect( 1.U )
                    dut.io.out.ship.general_id.general_owned.expect( 2.U )
                    dut.io.out.ship.fleet_hp.expect( 15.U )
                    dut.io.out.ship.scout_data.data_valid.expect( false.B )
                }

                //println(  "" + cycle + 
                //        ", dbg inc turret amnt " + dut.debug_observables.econ_inc_turret_hp_out.peekValue().asBigInt + 
                //        ", dbg combat turret hp out " + dut.debug_observables.combat_turret_hp_out.peekValue().asBigInt + 
                //        ", dbg general_wants_hp " + dut.debug_observables.general_wants_hp.peekValue().asBigInt +
                //        ", dbg ship_is_seen " + dut.debug_observables.ship_is_seen.peekValue().asBigInt +
                //        ", dbg max_hp_seen_by_combat " + dut.debug_observables.max_hp_seen_by_combat.peekValue().asBigInt )

                dut.clock.step()
                cycle += 1
            }

        }
    }
}
