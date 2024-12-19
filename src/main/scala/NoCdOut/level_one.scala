package level_desc 

import chisel3._
import chisel3.util.Decoupled

import common._
import ships._
import general._
import planet._
import noc._
import levelbuild._ 

class levelOneDescription {

    //HELLO AND WELCOME TO GLORIOUS PACKET COMBAT
    //to play, select the order of generals to fight on this galactic stage

    // YOU WILL BE FACING OFF AGAINST THE MOST POWERFUL OF OPPONENTS TO WALK THIS EARTH
    // Prepare adequately...

    // However, at your side are 5 ~~competent~~ generals to fight with you!

    // First, we have the valiant William, here for a long time, not a long time.
    // Second, we have Andy, here for a good time, not a long time.
    // Third, Skippy, who believes more fodder is better.
    // Fourth, SpaceManMike, here to conquer the galaxies with fair, honest, combat
    // and last but not least, Oobleck, who, well, made it through general academy so it's your problem now.

    val general_map : Map[String, Int] = Map( //assign their locations from 0-4!
        "William" ->        0,
        "Andy" ->           1,
        "Skippy" ->         2,
        "SpaceManMike" ->   3,
        "Oobleck" ->        4
    )
    // Generals are allocated to a planet and will be waiting at high command until their planet is captured, 
    // after which point, they will take command.

    //The map you will be playing on
    //planets will be identified by (starting team [Friendly/Hostile/None], Planet Identifier)
    //Empty space is denoted by ****
    /* 
    ****|****| H4 | H3 
     F0 |****|****|****
     F1 |****|****|****
    ****|****| N2 |****
     */
    // our scanners report that planet 2 is very rich in resources! capturing it will be of great use!

    val input_params    = new GameParametersInput
        {
            noc_x_size          = 0x4 // Need smaller size for sanity's sake
            noc_y_size          = 0x4 
        }

    val params  = new GameParameters( input_params )

    val Timur1 = new TimurBuilder( params, 5 )
    val Timur2 = new TimurBuilder( params, 6 )
    val Timur3 = new TimurBuilder( params, 7 )
    val Timur4 = new TimurBuilder( params, 8 )
    val Timur5 = new TimurBuilder( params, 9 )
    
    val where_planets = Seq(Seq(0,1), Seq(0,2), Seq(2,3), Seq(3,0), Seq(2,0))

    object GeneralFactory{
        def create(generalName: String, index: Int): GeneralBuilder = generalName match{
            case "William" => new WilliamBuilder(params, index){
                my_x_pos = where_planets(index)(0)
                my_y_pos = where_planets(index)(1)}
            case "Andy" => new AndyBuilder(params, index){
                my_x_pos = where_planets(index)(0)
                my_y_pos = where_planets(index)(1)}
            case "SpaceManMike" => new SpaceManMikeBuilder(params, index){
                my_x_pos = where_planets(index)(0)
                my_y_pos = where_planets(index)(1)}
            case "Oobleck" => new OobleckBuilder(params, index){
                my_x_pos = where_planets(index)(0)
                my_y_pos = where_planets(index)(1)}
            case "Skippy" => new SkippyBuilder(params, index){
                my_x_pos = where_planets(index)(0)
                my_y_pos = where_planets(index)(1)}
        }
    }

    val your_generals = general_map.map{case (key, index) => GeneralFactory.create(key, index)}.toSeq
    val enemy_generals = List[GeneralBuilder](Timur1, Timur2, Timur3, Timur4, Timur5)

    val general_builders0   = List[GeneralBuilder]( your_generals(0), enemy_generals(0) )
    val general_ids0        = List[Int]( 0, 5 )
    
    val general_builders1   = List[GeneralBuilder]( your_generals(1), enemy_generals(1) )
    val general_ids1        = List[Int]( 1, 6 )
    
    val general_builders2   = List[GeneralBuilder]( your_generals(2), enemy_generals(2) )
    val general_ids2        = List[Int]( 2, 7 )
    
    val general_builders3   = List[GeneralBuilder]( your_generals(3), enemy_generals(3) )
    val general_ids3        = List[Int]( 3, 8 )
    
    val general_builders4   = List[GeneralBuilder]( your_generals(4), enemy_generals(4) )
    val general_ids4        = List[Int]( 4, 9 )
    
    
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
        x_pos                       = where_planets(0)(0)
        y_pos                       = where_planets(0)(1)
    }

    val descriptor1 = new PlanetDescriptor
    {
        resource_production_rate    = 10
        max_resources               = 40
        // game parameters will be supplied externally
        general_builders            = general_builders1
        general_ids                 = general_ids1
        default_team                = 0
        owned_by_default            = true
        buffer_depth                = 2
        x_pos                       = where_planets(1)(0)
        y_pos                       = where_planets(1)(1)
    }

     val descriptor2 = new PlanetDescriptor
    {
        resource_production_rate    = 12
        max_resources               = 120
        // game parameters will be supplied externally
        general_builders            = general_builders2
        general_ids                 = general_ids2
        default_team                = 0
        owned_by_default            = false
        buffer_depth                = 2
        x_pos                       = where_planets(2)(0)
        y_pos                       = where_planets(2)(1)
    }
    
     val descriptor3 = new PlanetDescriptor
    {
        resource_production_rate    = 8
        max_resources               = 40
        // game parameters will be supplied externally
        general_builders            = general_builders3
        general_ids                 = general_ids3
        default_team                = 1
        owned_by_default            = true
        buffer_depth                = 2
        x_pos                       = where_planets(3)(0)
        y_pos                       = where_planets(3)(1)
    }

     val descriptor4 = new PlanetDescriptor
    {
        resource_production_rate    = 4
        max_resources               = 80
        // game parameters will be supplied externally
        general_builders            = general_builders4
        general_ids                 = general_ids4
        default_team                = 1
        owned_by_default            = true
        buffer_depth                = 2
        x_pos                       = where_planets(4)(0)
        y_pos                       = where_planets(4)(1)
    }
    
    val descriptors = List[PlanetDescriptor]( descriptor0, descriptor1, descriptor2, descriptor3, descriptor4 )    

}
