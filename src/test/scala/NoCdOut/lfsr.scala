

package lfsr

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class lfsr_test extends AnyFreeSpec with Matchers {

    "lfsr4" in {
        simulate(new lfsr4( List( 1.U(1.W), 0.U(1.W), 0.U(1.W), 1.U(1.W) ) )) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            //println( "value is " + dut.io.out.peekValue().asBigInt )
            dut.clock.step()

            var cycles = 0

            while (cycles <= 32) 
            {
                //println( "value is " + dut.io.out.peekValue().asBigInt )

                // Step the simulation forward.
                dut.clock.step()
                cycles += 1
            }
        }
    }

    "lfsr8" in {
        simulate(new lfsr8( List( 0.U(1.W), 1.U(1.W), 1.U(1.W), 0.U(1.W), 1.U(1.W), 0.U(1.W), 0.U(1.W), 1.U(1.W) ) )) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            //println( "value is " + dut.io.out.peekValue().asBigInt )
            dut.clock.step()

            var cycles = 0

            while (cycles <= 128) 
            {
                //println( "value is " + dut.io.out.peekValue().asBigInt )

                // Step the simulation forward.
                dut.clock.step()
                cycles += 1
            }
        }
    }

    "lfsr16" in {
        simulate(new lfsr16( List( 0.U(1.W), 1.U(1.W), 1.U(1.W), 0.U(1.W), 1.U(1.W), 0.U(1.W), 0.U(1.W), 1.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W) ) )) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            //println( "value is " + dut.io.out.peekValue().asBigInt )
            dut.clock.step()

            var cycles = 0

            while (cycles <= 128) 
            {
                //println( "value is " + dut.io.out.peekValue().asBigInt )

                // Step the simulation forward.
                dut.clock.step()
                cycles += 1
            }
        }
    }

    "lfsr32" in {
        simulate(new lfsr32( List(  0.U(1.W), 1.U(1.W), 1.U(1.W), 0.U(1.W), 1.U(1.W), 0.U(1.W), 0.U(1.W), 1.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W),
                                    0.U(1.W), 1.U(1.W), 1.U(1.W), 0.U(1.W), 1.U(1.W), 0.U(1.W), 0.U(1.W), 1.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W), 0.U(1.W) ) )) 
        {
            dut =>

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            //println( "value is " + dut.io.out.peekValue().asBigInt )
            dut.clock.step()

            var cycles = 0

            while (cycles <= 128) 
            {
                //println( "value is " + dut.io.out.peekValue().asBigInt )

                // Step the simulation forward.
                dut.clock.step()
                cycles += 1
            }
        }
    }
}
