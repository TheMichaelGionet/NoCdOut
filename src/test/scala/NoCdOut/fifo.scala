

package common

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class fifo_test extends AnyFreeSpec with Matchers {

    "fifo" in {
        simulate(new FIFO( 4, UInt(16.W) )) 
        {
            dut =>

            dut.io.out.ready.poke( false.B )
            dut.io.in.valid.poke( false.B )
            dut.io.in.bits.poke( 0.U )
            
            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)
            //println( "value is " + dut.io.out.peekValue().asBigInt )
            
            dut.io.out.ready.poke( false.B )
            dut.io.in.valid.poke( true.B )
            dut.io.in.bits.poke( 69.U )
            
            //println( "69" )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect(true.B)
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect(false.B)
            
            dut.clock.step()
            
            //println( "420" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect( true.B )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect( true.B )
            
            dut.io.out.bits.expect( 69.U )
            
            dut.io.in.bits.poke( 420.U )
            
            dut.clock.step()
            
            // I ran out of good numbers :(
            dut.io.in.bits.poke( 1.U )
            
            //println( "1" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect( true.B )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect( true.B )
            
            dut.clock.step()
            
            dut.io.in.bits.poke( 10.U )
            //println( "10" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect( true.B )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect( true.B )
            
            dut.clock.step()
            
            //println( "<69" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect( true.B )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect( false.B )
            
            dut.io.in.valid.poke( false.B )
            
            dut.io.out.ready.poke( true.B )
            
            dut.io.out.bits.expect( 69.U )
            
            dut.clock.step()
            
            //println( "<420" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect( true.B )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect( true.B )
            
            dut.io.out.bits.expect( 420.U )
            
            dut.clock.step()
            
            //println( "<1" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect( true.B )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect( true.B )
            
            dut.io.out.bits.expect( 1.U )

            dut.clock.step()
            
            //println( "<10" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            dut.io.out.valid.expect( true.B )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.in.ready.expect( true.B )
            
            dut.io.out.bits.expect( 10.U )

            dut.clock.step()
            
            //println( "none" )
            //println( "dut.io.out.valid " + dut.io.out.valid.peekValue().asBigInt )
            //println( "dut.io.in.ready " + dut.io.in.ready.peekValue().asBigInt )
            dut.io.out.valid.expect( false.B )
            dut.io.in.ready.expect( true.B )

            
        }
    }
}
