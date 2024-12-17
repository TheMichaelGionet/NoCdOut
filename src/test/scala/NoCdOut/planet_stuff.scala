

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
}
