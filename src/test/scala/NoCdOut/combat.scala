


package tests

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._
import noc._

class combat_test extends AnyFreeSpec with Matchers {

    "blowy-uppy" in {

        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )
                
        simulate( new FightPerSide( params ) ) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            dut.clock.step()

            dut.io.ins(1).valid.poke( true.B )
            dut.io.ins(1).bits.src.x.poke(2.U)
            dut.io.ins(1).bits.src.y.poke(0.U)
            dut.io.ins(1).bits.dst.x.poke(0.U)
            dut.io.ins(1).bits.dst.y.poke(0.U)
            dut.io.ins(1).bits.general_id.side.poke(1.U)
            dut.io.ins(1).bits.general_id.general_owned.poke(0.U)
            dut.io.ins(1).bits.ship_class.poke(1.U)
            dut.io.ins(1).bits.fleet_hp.poke(10.U)
            dut.io.ins(1).bits.scout_data.data_valid.poke(false.B)
            dut.io.ins(1).bits.scout_data.loc.x.poke(0.U)
            dut.io.ins(1).bits.scout_data.loc.y.poke(0.U)
            dut.io.ins(1).bits.scout_data.side.poke(0.U)
            //dut.io.packets_in.in_n.ready.poke(true.B)

            dut.io.ins(0).valid.poke( true.B )
            dut.io.ins(0).bits.src.x.poke(4.U)
            dut.io.ins(0).bits.src.y.poke(0.U)
            dut.io.ins(0).bits.dst.x.poke(0.U)
            dut.io.ins(0).bits.dst.y.poke(0.U)
            dut.io.ins(0).bits.general_id.side.poke(0.U)
            dut.io.ins(0).bits.general_id.general_owned.poke(0.U)
            dut.io.ins(0).bits.ship_class.poke(0.U)
            dut.io.ins(0).bits.fleet_hp.poke(17.U)
            dut.io.ins(0).bits.scout_data.data_valid.poke(false.B)
            dut.io.ins(0).bits.scout_data.loc.x.poke(0.U)
            dut.io.ins(0).bits.scout_data.loc.y.poke(0.U)
            dut.io.ins(0).bits.scout_data.side.poke(0.U)

            // dut.io.packets_out.out_w.ready.poke(true.B)
            // dut.io.packets_out.out_n.ready.poke(true.B)
            // dut.io.packets_out.out_e.ready.poke(true.B)
            // dut.io.packets_out.out_s.ready.poke(true.B)
            // dut.io.packets_out.out_p.ready.poke(true.B)

            println("out_valid is " + dut.io.out.valid.peekValue().asBigInt)
            println("out_str is " + dut.io.out.bits.fleet_hp.peekValue().asBigInt)
            println("out_src_x is " + dut.io.out.bits.src.x.peekValue().asBigInt)
            // println("e_out_valid is " + dut.io.packets_out.out_e.valid.peekValue().asBigInt)
            // println("w_out_valid is " + dut.io.packets_out.out_w.valid.peekValue().asBigInt)
            // println("p_out_valid is " + dut.io.packets_out.out_p.valid.peekValue().asBigInt)

            // println("n_in_ready is " + dut.io.packets_in.in_n.ready.peekValue().asBigInt)
            // println("s_in_ready is " + dut.io.packets_in.in_s.ready.peekValue().asBigInt)
            // println("e_in_ready is " + dut.io.packets_in.in_e.ready.peekValue().asBigInt)
            // println("w_in_ready is " + dut.io.packets_in.in_w.ready.peekValue().asBigInt)
            // println("p_in_ready is " + dut.io.planet_in.ready.peekValue().asBigInt)

            // println("n_out_src x is "  + dut.io.packets_out.out_n.bits.src.x.peekValue().asBigInt)
            // println("n_out_src y is "  + dut.io.packets_out.out_n.bits.src.y.peekValue().asBigInt)

            dut.clock.step()


            // println("w4n is " + dut.w4n.peekValue().asBigInt) // can't access vals :sob:
            // println("w2n is " + dut.w2n.peekValue().asBigInt)
            
        }
    }
    "world-at-war" in {

        val input_params    = new GameParametersInput
        {
            // Use defaults
        }

        val params  = new GameParameters( input_params )
                
        simulate( new FightGlobal( params ) ) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)

            dut.io.in_n.valid.poke( false.B )
            dut.io.in_n.bits.src.x.poke(4.U)
            dut.io.in_n.bits.src.y.poke(0.U)
            dut.io.in_n.bits.dst.x.poke(0.U)
            dut.io.in_n.bits.dst.y.poke(0.U)
            dut.io.in_n.bits.general_id.side.poke(0.U)
            dut.io.in_n.bits.general_id.general_owned.poke(0.U)
            dut.io.in_n.bits.ship_class.poke(0.U)
            dut.io.in_n.bits.fleet_hp.poke(0.U)
            dut.io.in_n.bits.scout_data.data_valid.poke(false.B)
            dut.io.in_n.bits.scout_data.loc.x.poke(0.U)
            dut.io.in_n.bits.scout_data.loc.y.poke(0.U)
            dut.io.in_n.bits.scout_data.side.poke(0.U)
            //dut.io.in_n.ready.poke(true.B)

            dut.io.in_e.valid.poke( false.B )
            dut.io.in_e.bits.src.x.poke(1.U)
            dut.io.in_e.bits.src.y.poke(2.U)
            dut.io.in_e.bits.dst.x.poke(0.U)
            dut.io.in_e.bits.dst.y.poke(0.U)
            dut.io.in_e.bits.general_id.side.poke(0.U)
            dut.io.in_e.bits.general_id.general_owned.poke(0.U)
            dut.io.in_e.bits.ship_class.poke(0.U)
            dut.io.in_e.bits.fleet_hp.poke(0.U)
            dut.io.in_e.bits.scout_data.data_valid.poke(false.B)
            dut.io.in_e.bits.scout_data.loc.x.poke(0.U)
            dut.io.in_e.bits.scout_data.loc.y.poke(0.U)
            dut.io.in_e.bits.scout_data.side.poke(0.U)
            //dut.io.in_e.ready.poke(true.B)

            dut.io.in_s.valid.poke( false.B )
            dut.io.in_s.bits.src.x.poke(3.U)
            dut.io.in_s.bits.src.y.poke(3.U)
            dut.io.in_s.bits.dst.x.poke(0.U)
            dut.io.in_s.bits.dst.y.poke(0.U)
            dut.io.in_s.bits.general_id.side.poke(0.U)
            dut.io.in_s.bits.general_id.general_owned.poke(0.U)
            dut.io.in_s.bits.ship_class.poke(0.U)
            dut.io.in_s.bits.fleet_hp.poke(0.U)
            dut.io.in_s.bits.scout_data.data_valid.poke(false.B)
            dut.io.in_s.bits.scout_data.loc.x.poke(0.U)
            dut.io.in_s.bits.scout_data.loc.y.poke(0.U)
            dut.io.in_s.bits.scout_data.side.poke(0.U)
            //dut.io.in_s.ready.poke(true.B)

            dut.io.in_w.valid.poke( true.B )
            dut.io.in_w.bits.src.x.poke(2.U)
            dut.io.in_w.bits.src.y.poke(2.U)
            dut.io.in_w.bits.dst.x.poke(2.U)
            dut.io.in_w.bits.dst.y.poke(3.U)
            dut.io.in_w.bits.general_id.side.poke(0.U)
            dut.io.in_w.bits.general_id.general_owned.poke(0.U)
            dut.io.in_w.bits.ship_class.poke(0.U)
            dut.io.in_w.bits.fleet_hp.poke(10.U)
            dut.io.in_w.bits.scout_data.data_valid.poke(false.B)
            dut.io.in_w.bits.scout_data.loc.x.poke(0.U)
            dut.io.in_w.bits.scout_data.loc.y.poke(0.U)
            dut.io.in_w.bits.scout_data.side.poke(0.U)
            //dut.io.in_w.ready.poke(true.B)
            
            dut.io.in_p.valid.poke( false.B )
            dut.io.in_p.bits.src.x.poke(1.U)
            dut.io.in_p.bits.src.y.poke(1.U)
            dut.io.in_p.bits.dst.x.poke(0.U)
            dut.io.in_p.bits.dst.y.poke(0.U)
            dut.io.in_p.bits.general_id.side.poke(0.U)
            dut.io.in_p.bits.general_id.general_owned.poke(0.U)
            dut.io.in_p.bits.ship_class.poke(0.U)
            dut.io.in_p.bits.fleet_hp.poke(0.U)
            dut.io.in_p.bits.scout_data.data_valid.poke(false.B)
            dut.io.in_p.bits.scout_data.loc.x.poke(0.U)
            dut.io.in_p.bits.scout_data.loc.y.poke(0.U)
            dut.io.in_p.bits.scout_data.side.poke(0.U)
            //dut.io.in_p.ready.poke(true.B)

            dut.io.out.out_w.ready.poke(true.B)
            dut.io.out.out_n.ready.poke(true.B)
            dut.io.out.out_e.ready.poke(true.B)
            dut.io.out.out_s.ready.poke(true.B)
            dut.io.out.out_p.ready.poke(true.B)

            println("GLOBAL COMBAT TEST PRINTOUTS ====")
            println("n_out_valid is " + dut.io.out.out_n.valid.peekValue().asBigInt)
            println("s_out_valid is " + dut.io.out.out_s.valid.peekValue().asBigInt)
            println("e_out_valid is " + dut.io.out.out_e.valid.peekValue().asBigInt)
            println("w_out_valid is " + dut.io.out.out_w.valid.peekValue().asBigInt)
            println("p_out_valid is " + dut.io.out.out_p.valid.peekValue().asBigInt)

            // println("n_in_ready is " + dut.io.in_n.ready.peekValue().asBigInt)
            // println("s_in_ready is " + dut.io.in_s.ready.peekValue().asBigInt)
            // println("e_in_ready is " + dut.io.in_e.ready.peekValue().asBigInt)
            // println("w_in_ready is " + dut.io.in_w.ready.peekValue().asBigInt)
            // println("p_in_ready is " + dut.io.in_p.ready.peekValue().asBigInt)

            println("n_out_src x is "  + dut.io.out.out_n.bits.src.x.peekValue().asBigInt)
            println("n_out_src y is "  + dut.io.out.out_n.bits.src.y.peekValue().asBigInt)
            println("GLOBAL COMBAT TEST PRINTOUTS END")

            dut.clock.step()


            // println("w4n is " + dut.w4n.peekValue().asBigInt) // can't access vals :sob:
            // println("w2n is " + dut.w2n.peekValue().asBigInt)
            
        }
    }
}
