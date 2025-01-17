
package planet

import chisel3._
import chisel3.util.Decoupled

import common._
import ships._
import general._


class SectorBundle( params : GameParameters ) extends Bundle
{
    val in = new Bundle
    {
        val bp          = Output( Bool() )
        val ship        = Input( new Ship( params ) )
        val ship_valid  = Input( Bool() )
    }
    val out = new Bundle
    {
        val bp          = Input( Bool() )
        val ship        = Output( new Ship( params ) )
        val ship_valid  = Output( Bool() )
    }
}

class SectorBundleObserve( params : GameParameters ) extends Bundle
{
    val in = new Bundle
    {
        val bp          = Output( Bool() )
        val ship        = Output( new Ship( params ) )
        val ship_valid  = Output( Bool() )
    }
    val out = new Bundle
    {
        val bp          = Output( Bool() )
        val ship        = Output( new Ship( params ) )
        val ship_valid  = Output( Bool() )
    }
}

abstract class Sector( params : GameParameters, buffer_depth : Int ) extends Module
{
    val io = IO( new SectorBundle( params ) )

    val input_buffer    = Module( new FIFO( buffer_depth, new Ship( params ) ) )
    val output_buffer   = Module( new FIFO( buffer_depth, new Ship( params ) ) )
    
    input_buffer.io.in.valid    := io.in.ship_valid
    input_buffer.io.in.bits     := io.in.ship
    io.in.bp                    := !input_buffer.io.in.ready
    
    io.out.ship_valid           := output_buffer.io.out.valid
    io.out.ship                 := output_buffer.io.out.bits
    output_buffer.io.out.ready  := !io.out.bp
}

class EmptySpace( params : GameParameters, buffer_depth : Int ) extends Sector( params, buffer_depth )
{
    output_buffer.io.in.valid   := input_buffer.io.out.valid
    input_buffer.io.out.ready   := output_buffer.io.in.ready
    
    /*
        val src             = Output(new Coordinates( params.noc_x_size_len, params.noc_y_size_len ))
        val dst             = Output(new Coordinates( params.noc_x_size_len, params.noc_y_size_len ))
        val general_id      = Output(new GeneralID( params.num_players_len, params.num_generals_len ))
        val ship_class      = Output(UInt( params.num_ship_classes_len.W ))
        val fleet_hp        = Output(UInt( params.max_fleet_hp_len.W ))
        val scout_data      = Output(new ScoutData( params.noc_x_size_len, params.noc_y_size_len, params.num_players_len ))
    */
    output_buffer.io.in.bits.dst            := input_buffer.io.out.bits.src // swap src and dst to route ships back
    output_buffer.io.in.bits.src            := input_buffer.io.out.bits.dst
    output_buffer.io.in.bits.general_id     := input_buffer.io.out.bits.general_id
    output_buffer.io.in.bits.ship_class     := input_buffer.io.out.bits.ship_class
    output_buffer.io.in.bits.fleet_hp       := input_buffer.io.out.bits.fleet_hp
    output_buffer.io.in.bits.scout_data     := input_buffer.io.out.bits.scout_data
}

class ShipEventHandler( params : GameParameters, x_pos : Int, y_pos : Int ) extends Module
{
    val io = IO( new Bundle
    {
        val ship                = Input( new Ship( params ) )
        val ship_valid          = Input( Bool() )

        val new_ship            = Input( new Ship( params ) )
        val new_ship_valid      = Input( Bool() )
        
        val new_route           = Input( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
        
        val is_owned            = Input( Bool() ) // the planet, that is
        val owner               = Input( new GeneralID( params.num_players_len, params.num_generals_len ) )
        
        val ship_out            = Output( new Ship( params ) )
        val ship_out_valid      = Output( Bool() )
        
        val do_damage           = Output( Bool() )
        val consume_new_ship    = Output( Bool() )
    } )

    val here = Wire( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
    here.x  := x_pos.U
    here.y  := y_pos.U

    val ship_enemy              = !io.is_owned || ( io.ship.general_id.side =/= io.owner.side )
    val i_own_the_ship          = io.ship.general_id.general_owned === io.owner.general_owned && io.ship.general_id.side === io.owner.side

    val is_scout                = io.ship.ship_class    === ShipClasses.scout.U
    
    // default to internal, but prioritize external.
    // Reroute whenever it's friendly, or it's an enemy scout.
    val ship_multiplex          = ( io.ship_valid && ( !ship_enemy || is_scout ) )
    
    io.do_damage                := ship_enemy && !is_scout && io.ship_valid
    
    io.ship_out_valid           := io.new_ship_valid    || ship_multiplex
    
    val multiplexed_ship        = Mux( ship_multiplex, io.ship, io.new_ship )
    
    io.consume_new_ship         := !ship_multiplex && io.new_ship_valid
    
    io.ship_out.general_id      := multiplexed_ship.general_id
    io.ship_out.ship_class      := multiplexed_ship.ship_class
    io.ship_out.fleet_hp        := multiplexed_ship.fleet_hp
    
    val no_data                 = Wire( new ScoutData( params.noc_x_size_len, params.noc_y_size_len, params.num_players_len ) )
    no_data.data_valid          := false.B
    no_data.loc.x               := 0.U
    no_data.loc.y               := 0.U
    no_data.side                := 0.U
    no_data.owned               := false.B

    val this_planet_data        = Wire( new ScoutData( params.noc_x_size_len, params.noc_y_size_len, params.num_players_len ) )
    this_planet_data.data_valid := true.B
    this_planet_data.loc.x      := x_pos.U
    this_planet_data.loc.y      := y_pos.U
    this_planet_data.side       := io.owner.side
    this_planet_data.owned      := io.is_owned
    
    when( ( ship_multiplex && i_own_the_ship ) || !ship_multiplex )
    {
        io.ship_out.src         := here
        io.ship_out.dst         := io.new_route
        
        io.ship_out.scout_data  := no_data
    }
    .elsewhen( ship_multiplex && !ship_enemy && !i_own_the_ship || io.ship_valid && ship_enemy && is_scout )
    {
        io.ship_out.dst         := multiplexed_ship.src
        io.ship_out.src         := multiplexed_ship.dst
        
        io.ship_out.scout_data  := Mux( is_scout, this_planet_data, no_data )
    }
    .otherwise // when( ship_enemy && !is_scout )
    {
        // don't care, no routing. 
        io.ship_out.src         := here
        io.ship_out.dst         := here
        
        io.ship_out.scout_data  := no_data
    }
}

class CombatHandler( params : GameParameters ) extends Module
{
    val io = IO( new Bundle
    {
        val ship            = Input( new Ship( params ) )
        
        val do_damage       = Input( Bool() )
        
        val current_hp      = Input( UInt( params.max_turret_hp_len.W ) )
        val max_hp          = Input( UInt( params.max_turret_hp_len.W ) )
        
        val hp_inc          = Input( UInt( params.max_turret_hp_len.W ) )
        
        val new_hp          = Output( UInt( params.max_turret_hp_len.W ) )
        
        val planet_takeover = Output( Bool() )
        
        val new_side        = Output( UInt( params.num_players_len.W ) )
    } )
    
    val ship_lut            = Module( new ShipLut( params ) )
    
    ship_lut.io.ship_type   := io.ship.ship_class

    val log_attack_per_hp   = ship_lut.io.ship_stats.log_attack_per_hp
    
    val damage_from_ship    = Mux( log_attack_per_hp >= 0.S, io.ship.fleet_hp << log_attack_per_hp.asUInt, io.ship.fleet_hp >> (-log_attack_per_hp).asUInt )
    
    val damage              = Wire( SInt( (params.max_turret_hp_len+1).W ) )
    damage                  := Mux( io.do_damage, damage_from_ship.asSInt, 0.S )
    
    
    val current_hp          = Wire( SInt( (params.max_turret_hp_len+1).W ) )
    current_hp              := io.current_hp.asSInt
    
    
    val hp_inc              = Wire( SInt( (params.max_turret_hp_len+1).W ) )
    hp_inc                  := io.hp_inc.asSInt
    
    
    val unrectified_hp      = Wire( SInt( (params.max_turret_hp_len+2).W ) )
    unrectified_hp          := current_hp +& hp_inc -& damage
    
    
    val max_hp              = Wire( UInt( (params.max_turret_hp_len+1).W ) )
    max_hp                  := io.max_hp
    
    
    val final_hp            = Wire( UInt( params.max_turret_hp_len.W ) )
    final_hp                := Mux( unrectified_hp > max_hp.asSInt, io.max_hp, Mux( unrectified_hp < 0.S, 0.U, unrectified_hp.asUInt ) )
    
    
    io.new_hp               := final_hp
    
    io.planet_takeover      := ( unrectified_hp <= 0.S ) && io.do_damage
    
    io.new_side             := io.ship.general_id.side
}


class EconomyHandler( params : GameParameters ) extends Module
{
    val io = IO( new Bundle
    {
        val do_build_ship       = Input( Bool() )
        val which_ship          = Input( UInt( params.num_ship_classes_len.W ) )
        val how_many_ships      = Input( UInt( params.max_fleet_hp_len.W ) )
        
        val add_turret_hp       = Input( Bool() )
        
        val turret_hp_amount    = Input( UInt( params.max_turret_hp_len.W ) )
        
        val general_id          = Input( GeneralID( params ) )
        
        val resources           = Input( UInt( params.max_resource_val_len.W ) )
        val max_resources       = Input( UInt( params.max_resource_val_len.W ) )
        val add_resources       = Input( UInt( params.max_resource_val_len.W ) )
        
        val ship_backpressure   = Input( Bool() )
        val ship                = Output( new Ship( params ) )
        val ship_valid          = Output( Bool() )
        
        val inc_turret_hp_out   = Output( UInt( params.max_turret_hp_len.W ) )
        
        val resources_after_purchases   = Output( UInt( params.max_resource_val_len.W ) )
    } )
    
    // Rectify how many ships to buy
    
    val ship_lut    = Module( new ShipLut( params ) )
    
    ship_lut.io.ship_type       := io.which_ship
    
    val rectified_ship_amounts  = Mux( io.how_many_ships < ship_lut.io.ship_stats.min_hp_buy, ship_lut.io.ship_stats.min_hp_buy, Mux( io.how_many_ships > ship_lut.io.ship_stats.max_hp_buy, ship_lut.io.ship_stats.max_hp_buy, io.how_many_ships ) )
    val ship_price              = rectified_ship_amounts *ship_lut.io.ship_stats.cost_per_hp
    
    // Rectify the amount of turret hp to add
    
    val turret_min          = 5.U
    val turret_max          = 15.U
    val turret_cost_per_hp  = 1.U
    
    val rectified_turret_amounts    = Mux( io.turret_hp_amount < turret_min, turret_min, Mux( io.turret_hp_amount > turret_max, turret_max, io.turret_hp_amount ) )
    val turret_price                = rectified_turret_amounts * turret_cost_per_hp
    
    // Decide if purchasing a ship is possible given the amount of funds
    
    val can_purchase_ship           = ( ship_price <= io.resources ) && !io.ship_backpressure
    val will_purchase_ship          = can_purchase_ship && io.do_build_ship
    
    // Decide if purchasing the turret hp given the ship purchase (if one exists) is possible given the amount of funds and whether or not it would overflow. .
    
    val can_purchase_turret_hp      = ( ( turret_price + Mux( will_purchase_ship, ship_price, 0.U ) ) <= io.resources ) && ( ( rectified_turret_amounts + io.turret_hp_amount ) <= params.max_turret_hp.U )
    val will_purchase_turret_hp     = can_purchase_turret_hp && io.add_turret_hp
    
    // Set Ship stuff
    
    io.ship_valid                       := will_purchase_ship
    
    io.ship.src.x                       := 0.U
    io.ship.src.y                       := 0.U
    io.ship.dst.x                       := 0.U
    io.ship.dst.y                       := 0.U
    io.ship.general_id.side             := io.general_id.side
    io.ship.general_id.general_owned    := io.general_id.general_owned
    io.ship.ship_class                  := io.which_ship
    io.ship.fleet_hp                    := rectified_ship_amounts
    io.ship.scout_data.data_valid       := false.B
    io.ship.scout_data.loc.x            := 0.U
    io.ship.scout_data.loc.y            := 0.U
    io.ship.scout_data.owned            := false.B
    io.ship.scout_data.side             := 0.U
    
    // Set turret hp stuff
    
    io.inc_turret_hp_out                := Mux( will_purchase_turret_hp, rectified_turret_amounts, 0.U )
    
    // Set resources
    
    val resource_delta                  = io.add_resources - Mux( will_purchase_ship, ship_price, 0.U ) - Mux( will_purchase_turret_hp, turret_price, 0.U )
    
    val new_resource                    = io.resources + resource_delta
    
    io.resources_after_purchases        := Mux( new_resource > io.max_resources, io.max_resources, new_resource )
}

class GeneralMux( params : GameParameters, general_builders : List[GeneralBuilder], general_ids : List[Int] ) extends Module
{
    val io = IO( new Bundle
    {
        val ship_it_sees        = Input( new Ship( params ) )
        val ship_valid          = Input( Bool() )
        
        val resources           = Input( UInt( params.max_resource_val_len.W ) )
        val limit_resources     = Input( UInt( params.max_resource_val_len.W ) )

        val turret_hp           = Input( UInt( params.max_turret_hp_len.W ) )
        val limit_turret_hp     = Input( UInt( params.max_turret_hp_len.W ) )

        val under_attack        = Input( Bool() )
        val ship_was_built      = Input( Bool() )


        val is_owned            = Input( Bool() )
        val owner               = Input( UInt( params.num_players_len.W ) ) // owner === side
        val owner_changed       = Input( Bool() )

        val do_build_ship       = Output( Bool() )
        val which_ship          = Output( UInt( params.num_ship_classes_len.W ) )
        val how_many_ships      = Output( UInt( params.max_fleet_hp_len.W ) )
        
        val command_where       = Output( new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) )
        
        val add_turret_hp       = Output( Bool() )
        val how_much_turret_hp  = Output( UInt( params.max_turret_hp_len.W ) )

        val general_id          = Output( UInt( params.num_generals_len.W ) ) // general_id === general_owned
    } )

    val generalzzz          = general_builders.map{ x => Module( x() ) }
    val general_id_regs     = RegInit( VecInit( general_ids.map{ x => x.U( params.num_generals_len.W ) } ) )
    
    io.general_id           := general_id_regs( io.owner )
    
    val do_build_ship       = Wire( Vec( params.num_players, Bool() ) )
    val which_ship          = Wire( Vec( params.num_players, UInt( params.num_ship_classes_len.W ) ) )
    val how_many_ships      = Wire( Vec( params.num_players, UInt( params.max_fleet_hp_len.W ) ) )
    
    val command_where       = Wire( Vec( params.num_players, new Coordinates( params.noc_x_size_len, params.noc_y_size_len ) ) )
    val add_turret_hp       = Wire( Vec( params.num_players, Bool() ) )
    val how_much_turret_hp  = Wire( Vec( params.num_players, UInt( params.max_turret_hp_len.W ) ) )

    var index = 0
    
    for( g <- generalzzz )
    {
        g.reset                     := ( this.reset.asBool || io.owner_changed ) // This just makes further logic less complicated within each general
        g.io.ship_it_sees           := io.ship_it_sees
        g.io.ship_valid             := io.ship_valid
        g.io.resources              := io.resources
        g.io.limit_resources        := io.limit_resources
        g.io.turret_hp              := io.turret_hp
        g.io.limit_turret_hp        := io.limit_turret_hp
        g.io.under_attack           := io.under_attack
        g.io.ship_was_built         := io.ship_was_built
        
        do_build_ship( index )      := g.io.do_build_ship
        which_ship( index )         := g.io.which_ship
        how_many_ships( index )     := g.io.how_many_ships
        command_where( index )      := g.io.command_where
        add_turret_hp( index )      := g.io.add_turret_hp
        how_much_turret_hp( index ) := g.io.how_much_turret_hp
        
        index += 1
    }

    io.do_build_ship        := Mux( io.is_owned, do_build_ship( io.owner ),         false.B )
    io.which_ship           := Mux( io.is_owned, which_ship( io.owner ),            0.U )
    io.how_many_ships       := Mux( io.is_owned, how_many_ships( io.owner ),        0.U )
    io.command_where.x      := Mux( io.is_owned, command_where( io.owner ).x,       0.U )
    io.command_where.y      := Mux( io.is_owned, command_where( io.owner ).y,       0.U )
    io.add_turret_hp        := Mux( io.is_owned, add_turret_hp( io.owner ),         0.U )
    io.how_much_turret_hp   := Mux( io.is_owned, how_much_turret_hp( io.owner ),    0.U )
}

class PlanetStateBundle( params : GameParameters ) extends Bundle
{
    val is_owned            = Output( Bool() )
    val owned_by            = Output( UInt( params.num_players_len.W ) )

    val resources           = Output( UInt( params.max_resource_val_len.W ) )
    val limit_resources     = Output( UInt( params.max_resource_val_len.W ) )
    val resource_prod       = Output( UInt( params.max_resource_val_len.W ) )

    val turret_hp           = Output( UInt( params.max_turret_hp_len.W ) )

    val ship_garrison       = Output( new Ship( params ) )
    val garrison_valid      = Output( Bool() )
}

class Planet(   resource_production_rate    : Int, 
                max_resources               : Int, 

                params                      : GameParameters, 

                general_builders            : List[GeneralBuilder],
                general_ids                 : List[Int],

                default_team                : Int,
                owned_by_default            : Boolean,

                buffer_depth                : Int,
                x_pos                       : Int, // the position on the NoC. 
                y_pos                       : Int
                ) extends Sector( params, buffer_depth )
{
    val state_observation   = IO( new PlanetStateBundle( params ) )

    val debug_observables   = IO( new Bundle
    {
        val econ_inc_turret_hp_out  = Output( UInt( params.max_turret_hp_len.W ) )
        val combat_turret_hp_out    = Output( UInt( params.max_turret_hp_len.W ) )
        val general_wants_hp        = Output( Bool() )
        val ship_is_seen            = Output( Bool() )
        val max_hp_seen_by_combat   = Output( UInt( params.max_turret_hp_len.W ) )
    } )
    
    // State stuff
    val is_owned            = RegInit( owned_by_default.B )
    val owned_by            = RegInit( default_team.U( params.num_players_len.W ) )

    val resources           = RegInit( 0.U( params.max_resource_val_len.W ) )
    val limit_resources     = RegInit( max_resources.U( params.max_resource_val_len.W ) )
    val resource_prod       = RegInit( resource_production_rate.U( params.max_resource_val_len.W ) )

    val turret_hp           = RegInit( 0.U( params.max_turret_hp_len.W ) )

    val ship_garrison       = Reg( new Ship( params ) )
    val garrison_valid      = RegInit( false.B )

    state_observation.is_owned          := is_owned
    state_observation.owned_by          := owned_by
    state_observation.resources         := resources
    state_observation.limit_resources   := limit_resources
    state_observation.resource_prod     := resource_prod
    state_observation.turret_hp         := turret_hp
    state_observation.ship_garrison     := ship_garrison
    state_observation.garrison_valid    := garrison_valid

    // Big components

    val ship_event_handler  = Module( new ShipEventHandler( params, x_pos, y_pos ) )

    val combat_handler      = Module( new CombatHandler( params ) )

    val economy_handler     = Module( new EconomyHandler( params ) )

    val general_mux         = Module( new GeneralMux( params, general_builders, general_ids ) )

    // Wiring

    val preserve_ships  = false
    if( preserve_ships )
    {
        input_buffer.io.out.ready   := output_buffer.io.in.ready
    }
    else
    {
        // Sometimes the output buffer might not be able to accept,
        // but now the game will not deadlock.
        // So ships can just get destroyed sometimes, and that can be okay. 
        // Canonically, if the atmosphere is cluttered, the closest ship just crash lands and dies. 
        input_buffer.io.out.ready   := true.B
    }

    general_mux.io.ship_it_sees     := input_buffer.io.out.bits
    general_mux.io.ship_valid       := input_buffer.io.out.valid
    general_mux.io.resources        := resources
    general_mux.io.limit_resources  := limit_resources
    general_mux.io.turret_hp        := turret_hp
    general_mux.io.limit_turret_hp  := params.max_turret_hp.U
    general_mux.io.under_attack     := combat_handler.io.do_damage
    general_mux.io.ship_was_built   := garrison_valid
    general_mux.io.is_owned         := is_owned
    general_mux.io.owner            := owned_by
    general_mux.io.owner_changed    := combat_handler.io.planet_takeover
    
    economy_handler.io.do_build_ship            := general_mux.io.do_build_ship
    economy_handler.io.which_ship               := general_mux.io.which_ship
    economy_handler.io.how_many_ships           := general_mux.io.how_many_ships
    economy_handler.io.add_turret_hp            := general_mux.io.add_turret_hp
    economy_handler.io.turret_hp_amount         := general_mux.io.how_much_turret_hp
    economy_handler.io.general_id.side          := owned_by
    economy_handler.io.general_id.general_owned := general_mux.io.general_id
    economy_handler.io.resources                := resources
    economy_handler.io.max_resources            := limit_resources
    economy_handler.io.add_resources            := resource_prod
    economy_handler.io.ship_backpressure        := garrison_valid && !ship_event_handler.io.consume_new_ship


    debug_observables.econ_inc_turret_hp_out    := economy_handler.io.inc_turret_hp_out
    debug_observables.combat_turret_hp_out      := combat_handler.io.new_hp
    debug_observables.general_wants_hp          := general_mux.io.add_turret_hp
    debug_observables.ship_is_seen              := input_buffer.io.out.valid
    debug_observables.max_hp_seen_by_combat     := combat_handler.io.max_hp

    resources                                   := economy_handler.io.resources_after_purchases

    when( economy_handler.io.ship_valid )
    {
        ship_garrison   := economy_handler.io.ship
        garrison_valid  := true.B
    }
    .elsewhen( ship_event_handler.io.consume_new_ship )
    {
        garrison_valid  := false.B
    }
    
    ship_event_handler.io.ship                  := input_buffer.io.out.bits
    ship_event_handler.io.ship_valid            := input_buffer.io.out.valid
    ship_event_handler.io.new_ship              := ship_garrison
    ship_event_handler.io.new_ship_valid        := garrison_valid
    ship_event_handler.io.new_route             := general_mux.io.command_where
    ship_event_handler.io.is_owned              := is_owned
    ship_event_handler.io.owner.side            := owned_by
    ship_event_handler.io.owner.general_owned   := general_mux.io.general_id
    
    output_buffer.io.in.bits                    := ship_event_handler.io.ship_out
    output_buffer.io.in.valid                   := ship_event_handler.io.ship_out_valid
    
    combat_handler.io.ship                      := input_buffer.io.out.bits
    combat_handler.io.do_damage                 := ship_event_handler.io.do_damage
    combat_handler.io.current_hp                := turret_hp
    combat_handler.io.max_hp                    := params.max_turret_hp.U
    combat_handler.io.hp_inc                    := economy_handler.io.inc_turret_hp_out

    turret_hp                                   := combat_handler.io.new_hp
    when( combat_handler.io.planet_takeover )
    {
        is_owned    := true.B
        owned_by    := combat_handler.io.new_side
    }

}

