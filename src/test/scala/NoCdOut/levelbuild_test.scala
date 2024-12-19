package levelbuild

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import common._
import ships._
import general._
import planet._


class levelbuild_test extends AnyFreeSpec with Matchers {


    "level build test" in {
        
        val input_params    = new GameParametersInput
        {
            noc_x_size          = 0x4 // Need smaller size for sanity's sake
            noc_y_size          = 0x4 
        }

        val params  = new GameParameters( input_params )

        val bill_dorr0 = new GeneralTestPlanetBuilder( params, 10, 1 )
        val bill_dorr1 = new GeneralTestPlanetBuilder( params, 15, 2 )
        
        val general_builders0   = List[GeneralBuilder]( bill_dorr0, bill_dorr1 )
        val general_ids0        = List[Int]( 1, 2 )
        
        val Jeff        = new GeneralJeffBuilder( params, 4 )
        val tester      = new GeneralTestBuilder( params, 15, 3 )
        
        val general_builders1   = List[GeneralBuilder]( tester, Jeff )
        val general_ids1        = List[Int]( 3, 4 )
        
        val descriptor0 = new PlanetDescriptor
        {
            resource_production_rate    = 5
            max_resources               = 80
            // game parameters will be supplied externally
            general_builders            = general_builders0
            general_ids                 = general_ids0
            default_team                = 0
            owned_by_default            = true
            buffer_depth                = 2
            x_pos                       = 0
            y_pos                       = 3
        }

        val descriptor1 = new PlanetDescriptor
        {
            resource_production_rate    = 10
            max_resources               = 40
            // game parameters will be supplied externally
            general_builders            = general_builders1
            general_ids                 = general_ids1
            default_team                = 1
            owned_by_default            = true
            buffer_depth                = 2
            x_pos                       = 1
            y_pos                       = 2
        }
        
        val descriptors = List[PlanetDescriptor]( descriptor0, descriptor1 )
        
        
        
        simulate( new LevelBuilder( params, 2, descriptors ) ) 
        {
            dut =>
            
            //dut.io.in.ship_valid.poke( false.B )
            //dut.io.out.bp.poke( false.B )

            for( x <- 0 until 4 )
            {
                for( y <- 0 until 4 )
                {
                    dut.io.le_vec(x)(y).out.bp.poke( false.B )
                    dut.io.le_vec(x)(y).in.ship_valid.poke( false.B )
                    
                    dut.io.le_vec(x)(y).in.ship.src.x.poke( 1.U )
                    dut.io.le_vec(x)(y).in.ship.src.y.poke( 2.U )
                    dut.io.le_vec(x)(y).in.ship.dst.x.poke( x.U )
                    dut.io.le_vec(x)(y).in.ship.dst.y.poke( y.U )
                    dut.io.le_vec(x)(y).in.ship.general_id.side.poke( 0.U )
                    dut.io.le_vec(x)(y).in.ship.general_id.general_owned.poke( 3.U )
                    dut.io.le_vec(x)(y).in.ship.ship_class.poke( 2.U )
                    dut.io.le_vec(x)(y).in.ship.fleet_hp.poke( 5.U )
                    dut.io.le_vec(x)(y).in.ship.scout_data.data_valid.poke( false.B )
                    dut.io.le_vec(x)(y).in.ship.scout_data.loc.x.poke( 0.U )
                    dut.io.le_vec(x)(y).in.ship.scout_data.loc.y.poke( 0.U )
                    dut.io.le_vec(x)(y).in.ship.scout_data.side.poke( 0.U )
                }
            }

            //dut.io.in.ship.src.x.poke( 3.U )
            //dut.io.in.ship.src.y.poke( 4.U )
            //dut.io.in.ship.dst.x.poke( 1.U )
            //dut.io.in.ship.dst.y.poke( 2.U )
            //dut.io.in.ship.general_id.side.poke( 0.U )
            //dut.io.in.ship.general_id.general_owned.poke( 3.U )
            //dut.io.in.ship.ship_class.poke( 2.U )
            //dut.io.in.ship.fleet_hp.poke( 5.U )
            //dut.io.in.ship.scout_data.data_valid.poke( true.B )
            //dut.io.in.ship.scout_data.loc.x.poke( 6.U )
            //dut.io.in.ship.scout_data.loc.y.poke( 7.U )
            //dut.io.in.ship.scout_data.side.poke( 2.U )

            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)

            for( x <- 0 until 4 )
            {
                for( y <- 0 until 4 )
                {
                    if( x == 0 && y == 3 )
                    {
                        dut.io.meta(x)(y).is_planet.expect(true.B)
                        dut.io.meta(x)(y).planet_index.expect(0.U)
                    }
                    else if( x == 1 && y == 2 )
                    {
                        dut.io.meta(x)(y).is_planet.expect(true.B)
                        dut.io.meta(x)(y).planet_index.expect(1.U)
                    }
                    else
                    {
                        dut.io.meta(x)(y).is_planet.expect(false.B)
                    }
                }
            }
            
            var cycle = 0
            
            def print_planet_map() = 
            {
                for( y <- 0 until 4 )
                {
                    var le_string = ""
                    for( x <- 0 until 4 )
                    {
                        le_string += "I " + dut.io.le_vec(x)(y).in.ship_valid.peekValue().asBigInt + ", O " + dut.io.le_vec(x)(y).out.ship_valid.peekValue().asBigInt + " | "
                    }
                    println( le_string )
                }
                println( "" )
            }
            
            def print_billdor0()  =
            {
                println( "billdor0: " + cycle + 
                    ", " + dut.state_observation.le_vec(0).is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.le_vec(0).resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).garrison_valid.peekValue().asBigInt )
            }
            
            def print_jeff() = 
            {
                println( "Jeff:      " + cycle + 
                    ", " + dut.state_observation.le_vec(1).is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.le_vec(1).resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).garrison_valid.peekValue().asBigInt )
            }
            
            //println( "d00d: cycle, is_owned, owned_by, resources, limit_resources, resource_prod, turret_hp, garrison_valid" )
            
            //print_billdor0()
            //print_jeff()
            //print_planet_map()

            dut.clock.step()
            cycle += 1
            
            while( cycle < 30 )
            {
                //print_billdor0()
                //print_jeff()
                //print_planet_map()
                
                dut.clock.step()
                cycle += 1
            }
        }
    }
    
    
    "level and NoC build test" in {
        
        val input_params    = new GameParametersInput
        {
            noc_x_size          = 0x4 // Need smaller size for sanity's sake
            noc_y_size          = 0x4 
        }

        val params  = new GameParameters( input_params )

        val bill_dorr0 = new GeneralTestPlanetBuilder( params, 10, 1 )
        val bill_dorr1 = new GeneralTestPlanetBuilder( params, 15, 2 )
        
        val general_builders0   = List[GeneralBuilder]( bill_dorr0, bill_dorr1 )
        val general_ids0        = List[Int]( 1, 2 )
        
        val Jeff        = new GeneralJeffBuilder( params, 4 )
        val tester      = new GeneralTestBuilder( params, 15, 3 )
        
        val general_builders1   = List[GeneralBuilder]( tester, Jeff )
        val general_ids1        = List[Int]( 3, 4 )
        
        val descriptor0 = new PlanetDescriptor
        {
            resource_production_rate    = 5
            max_resources               = 80
            // game parameters will be supplied externally
            general_builders            = general_builders0
            general_ids                 = general_ids0
            default_team                = 0
            owned_by_default            = true
            buffer_depth                = 2
            x_pos                       = 0
            y_pos                       = 3
        }

        val descriptor1 = new PlanetDescriptor
        {
            resource_production_rate    = 10
            max_resources               = 40
            // game parameters will be supplied externally
            general_builders            = general_builders1
            general_ids                 = general_ids1
            default_team                = 1
            owned_by_default            = true
            buffer_depth                = 2
            x_pos                       = 1
            y_pos                       = 2
        }
        
        val descriptors = List[PlanetDescriptor]( descriptor0, descriptor1 )
        
        
        
        simulate( new LevelNocBuilder( params, 2, descriptors ) ) 
        {
            dut =>
            
            dut.reset.poke(true.B)
            dut.clock.step()
            dut.reset.poke(false.B)

            for( x <- 0 until 4 )
            {
                for( y <- 0 until 4 )
                {
                    if( x == 0 && y == 3 )
                    {
                        dut.io.meta(x)(y).is_planet.expect(true.B)
                        dut.io.meta(x)(y).planet_index.expect(0.U)
                    }
                    else if( x == 1 && y == 2 )
                    {
                        dut.io.meta(x)(y).is_planet.expect(true.B)
                        dut.io.meta(x)(y).planet_index.expect(1.U)
                    }
                    else
                    {
                        dut.io.meta(x)(y).is_planet.expect(false.B)
                    }
                }
            }
            
            var cycle = 0
            
            def print_planet_map() = 
            {
                for( y <- 0 until 4 )
                {
                    var le_string = ""
                    for( x <- 0 until 4 )
                    {
                        le_string += "I " + dut.io.le_vec(x)(y).in.ship_valid.peekValue().asBigInt + ", O " + dut.io.le_vec(x)(y).out.ship_valid.peekValue().asBigInt + " | "
                    }
                    println( le_string )
                }
                println( "" )
            }
            
            def print_billdor0()  =
            {
                println( "billdor0: " + cycle + 
                    ", " + dut.state_observation.le_vec(0).is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.le_vec(0).resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(0).garrison_valid.peekValue().asBigInt )
            }
            
            def print_jeff() = 
            {
                println( "Jeff:     " + cycle + 
                    ", " + dut.state_observation.le_vec(1).is_owned.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).owned_by.peekValue().asBigInt + 
                    ", " + dut.state_observation.le_vec(1).resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).limit_resources.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).resource_prod.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).turret_hp.peekValue().asBigInt +
                    ", " + dut.state_observation.le_vec(1).garrison_valid.peekValue().asBigInt )
            }
            
            println( "d00d: cycle, is_owned, owned_by, resources, limit_resources, resource_prod, turret_hp, garrison_valid" )
            
            print_billdor0()
            print_jeff()
            print_planet_map()

            dut.clock.step()
            cycle += 1
            
            while( cycle < 30 )
            {
                print_billdor0()
                print_jeff()
                print_planet_map()
                
                dut.clock.step()
                cycle += 1
            }
        }
    }
}


