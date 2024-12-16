
package ships

import chisel3._
import chisel3.util.Decoupled

import common._

class ShipStats( params : GameParameters ) extends Bundle
{
    val log_attack_per_hp       = SInt( 8.W ) // attack = hp << log_attack_per_hp (or >> -log_attack_per_hp)
    val cost_per_hp             = UInt( 8.W ) 
    val min_hp_buy              = UInt( params.max_fleet_hp_len.W ) // Minimum a general can buy
    val max_hp_buy              = UInt( params.max_fleet_hp_len.W ) // Maximum a general can buy
}

class ShipLut( params : GameParameters ) extends Module
{
    val io = IO(new Bundle
    {
        val ship_type   = Input( UInt( bits.needed( ShipClasses.count-1 ).W ) )

        val ship_stats  = Output( new ShipStats( params ) )
    })
    
    val options         = Reg( Vec( ShipClasses.count, new ShipStats( params ) ) )
    
    options(ShipClasses.scout).log_attack_per_hp        := 0.S
    options(ShipClasses.scout).cost_per_hp              := 2.U
    options(ShipClasses.scout).min_hp_buy               := 5.U
    options(ShipClasses.scout).max_hp_buy               := 20.U
    
    options(ShipClasses.basic).log_attack_per_hp        := 1.S
    options(ShipClasses.basic).cost_per_hp              := 1.U
    options(ShipClasses.basic).min_hp_buy               := 5.U
    options(ShipClasses.basic).max_hp_buy               := 25.U
    
    options(ShipClasses.attack).log_attack_per_hp       := 4.S
    options(ShipClasses.attack).cost_per_hp             := 4.U
    options(ShipClasses.attack).min_hp_buy              := 5.U
    options(ShipClasses.attack).max_hp_buy              := 20.U
    
    options(ShipClasses.defence).log_attack_per_hp      := 0.S
    options(ShipClasses.defence).cost_per_hp            := 1.U
    options(ShipClasses.defence).min_hp_buy             := 20.U
    options(ShipClasses.defence).max_hp_buy             := 100.U
    
    options(ShipClasses.beefer).log_attack_per_hp       := 2.S
    options(ShipClasses.beefer).cost_per_hp             := 2.U
    options(ShipClasses.beefer).min_hp_buy              := 10.U
    options(ShipClasses.beefer).max_hp_buy              := 40.U
    
    options(ShipClasses.destroyer).log_attack_per_hp    := 4.S
    options(ShipClasses.destroyer).cost_per_hp          := 4.U
    options(ShipClasses.destroyer).min_hp_buy           := 20.U
    options(ShipClasses.destroyer).max_hp_buy           := 100.U
    
    io.ship_stats   := options(io.ship_type)
}

object TurretStats
{
    //val log_attack_per_hp       = insta kill ships because it takes less logic
    val cost_per_hp             = 1
    val min_hp_buy              = 5
    val max_hp_buy              = 20
}


