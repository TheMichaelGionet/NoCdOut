package noc

import chisel3._
import chisel3.util.Decoupled

import common._

class IOPerSide(params: GameParameters) extends Bundle {
    val in_n = Ship(params)//full bidirectionality!
    val in_w = Ship(params)
    val in_s = Ship(params)
    val in_e = Ship(params)
    val out_n = Ship(params)
    val out_w = Ship(params)
    val out_s = Ship(params)
    val out_e = Ship(params)
    val in_p = Ship(params)//planet/PE i/o -> maybe change this to be not per side? idk
    val out_p = Ship(params)
}

// class VCWrapper(params: GameParameters) extends Bundle {
//     private val vcs_temp = Seq[IOPerSide]()
//     for(i <- 0 until params.num_players){
//         vcs_temp ++= IOPerSide(params) //make sequence of IOPerSide bundles for VCs
//     }

// }

class Router(params: GameParameters) extends Module{
    val io = IO(new Bundle{
        val packets = IOPerSide(params)
    })
}



class NocSwitch(params: GameParameters, x: Int, y: Int) extends Module {
    val io = IO(new Bundle{
        val io = IOPerSide(params) // one physical channel!
    })

    val vcs = Seq[IOPerSide]()//bundle all virtual channels
    for(i <- 0 until params.num_players){
        vcs ++= IOPerSide(params) //make sequence of IOPerSide bundles for VCs
    }
    val vc_reg = Reg(vcs) // I am honestly not sure if this works but sure it does!

    
    

    //shoddy DOR for now
    for (i <- 0 until params.num_players){
        new Router(params)//route per vc
    }
    val Fn4s = (x != friendly.in_n.dst.x)//go straight
    val Fw4e = (y != friendly.in_w.dst.y)
    val Fw4s = ((y === friendly.in_w.dst.y) && (x != friendly.in_w.dst.x))//turn
    val Fn4p = (x === friendly.in_n.dst.x)//exit noc
    val Fw4p = ((x === friendly.in_w.dst.x) && (y === friendly.in_w.dst.y))
    val Fp4e = (y != friendly.in_p.dst.y)// planet 2 noc
    val Fp4s = ((y === friendly.in_p.dst.y) && (x != friendly.in_p.dst.x))
    val Fp4p = ((y === friendly.in_p.dst.y) && (x === friendly.in_p.dst.x))// I guess this corner case could exist with random destinations

    //enemy routing
    val En4s = (x != enemy.in_n.dst.x)//go straight
    val Ew4e = (y != enemy.in_w.dst.y)
    val Ew4s = ((y === enemy.in_w.dst.y) && (x != enemy.in_w.dst.x))//turn
    val En4p = (x === enemy.in_n.dst.x)//exit noc
    val Ew4p = ((x === enemy.in_w.dst.x) && (y === enemy.in_w.dst.y))
    val Ep4e = (y != enemy.in_p.dst.y)// planet 2 noc
    val Ep4s = ((y === enemy.in_p.dst.y) && (x != enemy.in_p.dst.x))
    val Ep4p = ((y === enemy.in_p.dst.y) && (x === enemy.in_p.dst.x))// I guess this corner case could exist with random destinations

    //COMBAT
    friendly_str = (Mux(friendly.in_n.valid, friendly.in_n.fleet_hp, 0.U) + Mux(friendly.in_w.valid, friendly.in_w.fleet_hp, 0.U) + Mux(friendly.in_p.valid, friendly.in_p.fleet_hp, 0.U))
    enemy_str = (Mux(enemy.in_n.valid, enemy.in_n.fleet_hp, 0.U) + Mux(enemy.in_w.valid, enemy.in_w.fleet_hp, 0.U) + Mux(enemy.in_p.valid, enemy.in_p.fleet_hp, 0.U))
    
    if((friendly_str != 0) && (enemy_str != 0)){//then we have a fight
        if(friendly_str > enemy_str){

        }
    }
}