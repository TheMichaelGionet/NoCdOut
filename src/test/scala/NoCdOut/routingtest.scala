

package tests

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._
import noc._

class router_test extends AnyFreeSpec with Matchers {

    "basics" in {

        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )
                
        simulate( new Router( params, 2.U, 2.U) ) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)

            dut.io.packets_in.in_n.valid.poke( false.B )
            dut.io.packets_in.in_n.bits.src.x.poke(0.U)
            dut.io.packets_in.in_n.bits.src.y.poke(0.U)
            dut.io.packets_in.in_n.bits.dst.x.poke(0.U)
            dut.io.packets_in.in_n.bits.dst.y.poke(0.U)
            dut.io.packets_in.in_n.bits.general_id.side.poke(0.U)
            dut.io.packets_in.in_n.bits.general_id.general_owned.poke(0.U)
            dut.io.packets_in.in_n.bits.ship_class.poke(0.U)
            dut.io.packets_in.in_n.bits.fleet_hp.poke(0.U)
            dut.io.packets_in.in_n.bits.scout_data.data_valid.poke(false.B)
            dut.io.packets_in.in_n.bits.scout_data.loc.x.poke(0.U)
            dut.io.packets_in.in_n.bits.scout_data.loc.y.poke(0.U)
            dut.io.packets_in.in_n.bits.scout_data.side.poke(0.U)
            //dut.io.packets_in.in_n.ready.poke(true.B)

            dut.io.packets_in.in_e.valid.poke( false.B )
            dut.io.packets_in.in_e.bits.src.x.poke(0.U)
            dut.io.packets_in.in_e.bits.src.y.poke(0.U)
            dut.io.packets_in.in_e.bits.dst.x.poke(0.U)
            dut.io.packets_in.in_e.bits.dst.y.poke(0.U)
            dut.io.packets_in.in_e.bits.general_id.side.poke(0.U)
            dut.io.packets_in.in_e.bits.general_id.general_owned.poke(0.U)
            dut.io.packets_in.in_e.bits.ship_class.poke(0.U)
            dut.io.packets_in.in_e.bits.fleet_hp.poke(0.U)
            dut.io.packets_in.in_e.bits.scout_data.data_valid.poke(false.B)
            dut.io.packets_in.in_e.bits.scout_data.loc.x.poke(0.U)
            dut.io.packets_in.in_e.bits.scout_data.loc.y.poke(0.U)
            dut.io.packets_in.in_e.bits.scout_data.side.poke(0.U)
            //dut.io.packets_in.in_e.ready.poke(true.B)

            dut.io.packets_in.in_s.valid.poke( false.B )
            dut.io.packets_in.in_s.bits.src.x.poke(0.U)
            dut.io.packets_in.in_s.bits.src.y.poke(0.U)
            dut.io.packets_in.in_s.bits.dst.x.poke(0.U)
            dut.io.packets_in.in_s.bits.dst.y.poke(0.U)
            dut.io.packets_in.in_s.bits.general_id.side.poke(0.U)
            dut.io.packets_in.in_s.bits.general_id.general_owned.poke(0.U)
            dut.io.packets_in.in_s.bits.ship_class.poke(0.U)
            dut.io.packets_in.in_s.bits.fleet_hp.poke(0.U)
            dut.io.packets_in.in_s.bits.scout_data.data_valid.poke(false.B)
            dut.io.packets_in.in_s.bits.scout_data.loc.x.poke(0.U)
            dut.io.packets_in.in_s.bits.scout_data.loc.y.poke(0.U)
            dut.io.packets_in.in_s.bits.scout_data.side.poke(0.U)
            //dut.io.packets_in.in_s.ready.poke(true.B)

            dut.io.packets_in.in_w.valid.poke( true.B )
            dut.io.packets_in.in_w.bits.src.x.poke(0.U)
            dut.io.packets_in.in_w.bits.src.y.poke(0.U)
            dut.io.packets_in.in_w.bits.dst.x.poke(2.U)
            dut.io.packets_in.in_w.bits.dst.y.poke(1.U)
            dut.io.packets_in.in_w.bits.general_id.side.poke(0.U)
            dut.io.packets_in.in_w.bits.general_id.general_owned.poke(0.U)
            dut.io.packets_in.in_w.bits.ship_class.poke(0.U)
            dut.io.packets_in.in_w.bits.fleet_hp.poke(0.U)
            dut.io.packets_in.in_w.bits.scout_data.data_valid.poke(false.B)
            dut.io.packets_in.in_w.bits.scout_data.loc.x.poke(0.U)
            dut.io.packets_in.in_w.bits.scout_data.loc.y.poke(0.U)
            dut.io.packets_in.in_w.bits.scout_data.side.poke(0.U)
            //dut.io.packets_in.in_w.ready.poke(true.B)
            
            dut.io.planet_in.valid.poke( false.B )
            dut.io.planet_in.bits.src.x.poke(0.U)
            dut.io.planet_in.bits.src.y.poke(0.U)
            dut.io.planet_in.bits.dst.x.poke(0.U)
            dut.io.planet_in.bits.dst.y.poke(0.U)
            dut.io.planet_in.bits.general_id.side.poke(0.U)
            dut.io.planet_in.bits.general_id.general_owned.poke(0.U)
            dut.io.planet_in.bits.ship_class.poke(0.U)
            dut.io.planet_in.bits.fleet_hp.poke(0.U)
            dut.io.planet_in.bits.scout_data.data_valid.poke(false.B)
            dut.io.planet_in.bits.scout_data.loc.x.poke(0.U)
            dut.io.planet_in.bits.scout_data.loc.y.poke(0.U)
            dut.io.planet_in.bits.scout_data.side.poke(0.U)
            //dut.io.planet_in.ready.poke(true.B)

            // dut.io.packets_out.out_w.valid.poke( 0.U )
            // dut.io.packets_out.out_w.src.x(0.U)
            // dut.io.packets_out.out_w.src.y(0.U)
            // dut.io.packets_out.out_w.dst.x(0.U)
            // dut.io.packets_out.out_w.dst.y(0.U)
            // dut.io.packets_out.out_w.general_id.side(0.U)
            // dut.io.packets_out.out_w.general_id.general_owned(0.U)
            // dut.io.packets_out.out_w.ship_class(0.U)
            // dut.io.packets_out.out_w.fleet_hp(0.U)
            // dut.io.packets_out.out_w.scout_data.data_valid(0.U)
            // dut.io.packets_out.out_w.scout_data.loc.x(0.U)
            // dut.io.packets_out.out_w.scout_data.loc.y(0.U)
            // dut.io.packets_out.out_w.scout_data.side(0.U)
            dut.io.packets_out.out_w.ready.poke(true.B)

            // dut.io.packets_out.out_n.valid.poke( 0.U )
            // dut.io.packets_out.out_n.src.x(0.U)
            // dut.io.packets_out.out_n.src.y(0.U)
            // dut.io.packets_out.out_n.dst.x(0.U)
            // dut.io.packets_out.out_n.dst.y(0.U)
            // dut.io.packets_out.out_n.general_id.side(0.U)
            // dut.io.packets_out.out_n.general_id.general_owned(0.U)
            // dut.io.packets_out.out_n.ship_class(0.U)
            // dut.io.packets_out.out_n.fleet_hp(0.U)
            // dut.io.packets_out.out_n.scout_data.data_valid(0.U)
            // dut.io.packets_out.out_n.scout_data.loc.x(0.U)
            // dut.io.packets_out.out_n.scout_data.loc.y(0.U)
            // dut.io.packets_out.out_n.scout_data.side(0.U)
            dut.io.packets_out.out_n.ready.poke(true.B)

            // dut.io.packets_out.out_e.valid.poke( 0.U )
            // dut.io.packets_out.out_e.src.x(0.U)
            // dut.io.packets_out.out_e.src.y(0.U)
            // dut.io.packets_out.out_e.dst.x(0.U)
            // dut.io.packets_out.out_e.dst.y(0.U)
            // dut.io.packets_out.out_e.general_id.side(0.U)
            // dut.io.packets_out.out_e.general_id.general_owned(0.U)
            // dut.io.packets_out.out_e.ship_class(0.U)
            // dut.io.packets_out.out_e.fleet_hp(0.U)
            // dut.io.packets_out.out_e.scout_data.data_valid(0.U)
            // dut.io.packets_out.out_e.scout_data.loc.x(0.U)
            // dut.io.packets_out.out_e.scout_data.loc.y(0.U)
            // dut.io.packets_out.out_e.scout_data.side(0.U)
            dut.io.packets_out.out_e.ready.poke(true.B)

            // dut.io.packets_out.out_s.valid.poke( 0.U )
            // dut.io.packets_out.out_s.src.x(0.U)
            // dut.io.packets_out.out_s.src.y(0.U)
            // dut.io.packets_out.out_s.dst.x(0.U)
            // dut.io.packets_out.out_s.dst.y(0.U)
            // dut.io.packets_out.out_s.general_id.side(0.U)
            // dut.io.packets_out.out_s.general_id.general_owned(0.U)
            // dut.io.packets_out.out_s.ship_class(0.U)
            // dut.io.packets_out.out_s.fleet_hp(0.U)
            // dut.io.packets_out.out_s.scout_data.data_valid(0.U)
            // dut.io.packets_out.out_s.scout_data.loc.x(0.U)
            // dut.io.packets_out.out_s.scout_data.loc.y(0.U)
            // dut.io.packets_out.out_s.scout_data.side(0.U)
            dut.io.packets_out.out_s.ready.poke(true.B)

            // dut.io.packets_out.out_p.valid.poke( 0.U )
            // dut.io.packets_out.out_p.src.x(0.U)
            // dut.io.packets_out.out_p.src.y(0.U)
            // dut.io.packets_out.out_p.dst.x(0.U)
            // dut.io.packets_out.out_p.dst.y(0.U)
            // dut.io.packets_out.out_p.general_id.side(0.U)
            // dut.io.packets_out.out_p.general_id.general_owned(0.U)
            // dut.io.packets_out.out_p.ship_class(0.U)
            // dut.io.packets_out.out_p.fleet_hp(0.U)
            // dut.io.packets_out.out_p.scout_data.data_valid(0.U)
            // dut.io.packets_out.out_p.scout_data.loc.x(0.U)
            // dut.io.packets_out.out_p.scout_data.loc.y(0.U)
            // dut.io.packets_out.out_p.scout_data.side(0.U)
            dut.io.packets_out.out_p.ready.poke(true.B)

            //dut.clock.step()

            println("n_out_valid is " + dut.io.packets_out.out_n.valid.peekValue().asBigInt)
            println("s_out_valid is " + dut.io.packets_out.out_s.valid.peekValue().asBigInt)
            println("e_out_valid is " + dut.io.packets_out.out_e.valid.peekValue().asBigInt)
            println("w_out_valid is " + dut.io.packets_out.out_w.valid.peekValue().asBigInt)
            println("p_out_valid is " + dut.io.packets_out.out_p.valid.peekValue().asBigInt)

            println("w_in_ready is " + dut.io.packets_in.in_w.ready.peekValue().asBigInt)

            // println("w4n is " + dut.w4n.peekValue().asBigInt) // can't access vals :sob:
            // println("w2n is " + dut.w2n.peekValue().asBigInt)
            
        }
    }
}
