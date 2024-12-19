package levelbuild

import chisel3._
import chisel3.util.Decoupled

import common._
import ships._
import general._
import planet._
import noc._

class PlanetDescriptor
{
    var resource_production_rate                    = 5
    var max_resources                               = 80
    // game parameters will be supplied externally
    var general_builders    : List[GeneralBuilder]  = List[GeneralBuilder]()
    var general_ids         : List[Int]             = List[Int]()
    var default_team                                = 0
    var owned_by_default                            = false
    var buffer_depth                                = 2
    var x_pos                                       = 0
    var y_pos                                       = 0
}

class ProtoCellBundle( n : Int ) extends Bundle
{
    val is_planet       = Output( Bool() )
    val planet_index    = Output( UInt( n.W ) )
}

class LevelBuilder( params : GameParameters, default_buffer_depth : Int, planet_descriptors : List[PlanetDescriptor] ) extends Module
{
    val number_of_planets   = planet_descriptors.length

    val io = IO( new Bundle
    {
        val le_vec  = Vec( params.noc_x_size, Vec( params.noc_y_size, new SectorBundle( params ) ) )
        val meta    = Vec( params.noc_x_size, Vec( params.noc_y_size, new ProtoCellBundle( bits.needed( planet_descriptors.length-1 ) ) ) )
    } )
    
    val state_observation = IO( new Bundle
    {
        val le_vec  = Vec( number_of_planets, new PlanetStateBundle( params ) )
    } )
    
    class ProtoCell
    {
        var is_planet       = false
        var planet_index    = 0
    }
    
    val generator_grid      = Array.tabulate( params.noc_x_size, params.noc_y_size )((_,_) => new ProtoCell )
    
    var index = 0
    for( p <- planet_descriptors )
    {
        generator_grid(p.x_pos)(p.y_pos).is_planet      = true
        generator_grid(p.x_pos)(p.y_pos).planet_index   = index
        index += 1
    }

    val populated_cells     = generator_grid.map{ 
                                row => row.map{ 
                                    cell => 
                                        if( cell.is_planet ) 
                                            Module( new Planet( 
                                                planet_descriptors(cell.planet_index).resource_production_rate,
                                                planet_descriptors(cell.planet_index).max_resources,
                                                params,
                                                planet_descriptors(cell.planet_index).general_builders,
                                                planet_descriptors(cell.planet_index).general_ids,
                                                planet_descriptors(cell.planet_index).default_team,
                                                planet_descriptors(cell.planet_index).owned_by_default,
                                                planet_descriptors(cell.planet_index).buffer_depth,
                                                planet_descriptors(cell.planet_index).x_pos,
                                                planet_descriptors(cell.planet_index).y_pos
                                            ) )
                                        else
                                            Module( new EmptySpace( params, default_buffer_depth ) )
        }
    }

    for( x <- 0 until params.noc_x_size )
    {
        for( y <- 0 until params.noc_y_size )
        {
            populated_cells(x)(y).io <> io.le_vec(x)(y)
            
            io.meta(x)(y).is_planet     := generator_grid(x)(y).is_planet.B
            io.meta(x)(y).planet_index  := generator_grid(x)(y).planet_index.U
        }
    }
    
    index = 0
    for( p <- planet_descriptors )
    {
        state_observation.le_vec( index ) <> populated_cells(p.x_pos)(p.y_pos).asInstanceOf[Planet].state_observation
        index += 1
    }
}

class LevelNocBuilder( params : GameParameters, default_buffer_depth : Int, planet_descriptors : List[PlanetDescriptor] ) extends Module
{
    val io = IO( new Bundle
    {
        val le_vec  = Vec( params.noc_x_size, Vec( params.noc_y_size, new SectorBundleObserve( params ) ) )
        val meta    = Vec( params.noc_x_size, Vec( params.noc_y_size, new ProtoCellBundle( bits.needed( planet_descriptors.length-1 ) ) ) )
    } )
    
    val state_observation = IO( new Bundle
    {
        val le_vec  = Vec( planet_descriptors.length, new PlanetStateBundle( params ) )
    } )
    
    val le_level    = Module( new LevelBuilder( params, default_buffer_depth, planet_descriptors ) )
    val le_noc      = Module( new NocBuilder( params ) )
    
    for( x <- 0 until params.noc_x_size )
    {
        for( y <- 0 until params.noc_y_size )
        {
            le_level.io.le_vec(x)(y).in <>  le_noc.io.planets(x)(y).out
            le_noc.io.planets(x)(y).in  <>  le_level.io.le_vec(x)(y).out
            
            io.meta(x)(y)               <> le_level.io.meta(x)(y)
            io.le_vec(x)(y)             := le_level.io.le_vec(x)(y)
        }
    }
    
    for( i <- 0 until planet_descriptors.length )
    {
        state_observation.le_vec( i )   := le_level.state_observation.le_vec( i )
    }
}

