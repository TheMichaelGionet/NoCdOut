package noc

import chisel3._
import chisel3.util.Decoupled

import common._

class InPerSide(params: GameParameters) extends Bundle {
    val in_n = new Ship(params)//full bidirectionality!
    val in_w = new Ship(params)
    val in_s = new Ship(params)
    val in_e = new Ship(params)
    val in_p = new Ship(params)//planet/PE i/o -> maybe change this to be not per side? idk
}

class OutPerSide(params: GameParameters) extends Bundle {
    val out_n = new Ship(params)
    val out_w = new Ship(params)
    val out_s = new Ship(params)
    val out_e = new Ship(params)
    val out_p = new Ship(params)
}

class Router(params: GameParameters) extends Module{
    val io = IO(new Bundle{
        val packets_in = new InPerSide(params)
        val packets_out = new OutPerSide(params)
    })
}



class NocSwitch(params: GameParameters, x: Int, y: Int) extends Module {
    val io = IO(new Bundle{
        val in = new InPerSide(params) // one physical channel!
        val out = new OutPerSide(params)
    })

    val vcs_in = Seq.fill(params.num_players)(Wire(new InPerSide(params)))
    val vc_in_reg = Reg(Vec(params.num_players, new InPerSide(params))) // I am honestly not sure if this works but sure it does!
    
    for(i <- 0 until params.num_players){//write to every VC, though overwrite the valid signals after >:)
        when(!vc_in_reg(i).in_n.backpressured && (io.in.in_n.general_id.side === i.U)){
            vc_in_reg(i).in_n := io.in.in_n
        }
        when(!vc_in_reg(i).in_w.backpressured && (io.in.in_w.general_id.side === i.U)){
            vc_in_reg(i).in_w := io.in.in_w
        }
        when(!vc_in_reg(i).in_e.backpressured && (io.in.in_e.general_id.side === i.U)){
            vc_in_reg(i).in_e := io.in.in_e
        }
        when(!vc_in_reg(i).in_s.backpressured && (io.in.in_s.general_id.side === i.U)){
            vc_in_reg(i).in_s := io.in.in_s
        }
        when(!vc_in_reg(i).in_p.backpressured && (io.in.in_p.general_id.side === i.U)){
            vc_in_reg(i).in_p := io.in.in_p
        }
        //vc_in_reg(i).elements.map{case (name, data) => vc_in_reg(i).elements(name).valid := (io.in.elements(name).general_id.side === i)}
    }

    // for ((fieldName, ship) <- io.in.elements){
    //     val targetVC = ship.general_id.side
    //     when(ship.valid && !(vc_in_reg(targetVC)(fieldName).backpressured)){//when we can update VC
    //         vc_in_reg(targetVC).elements(fieldName) := ship
    //     }
    // }

    //shoddy DOR for now
    val vc_routers = Seq[Router]()
    for (i <- 0 until params.num_players){
        vc_routers :+ Module(new Router(params))//route per vc
        vc_routers(i).io.packets_in := vc_in_reg(i)
    }
    // val Fn4s = (x != friendly.in_n.dst.x)//go straight
    // val Fw4e = (y != friendly.in_w.dst.y)
    // val Fw4s = ((y === friendly.in_w.dst.y) && (x != friendly.in_w.dst.x))//turn
    // val Fn4p = (x === friendly.in_n.dst.x)//exit noc
    // val Fw4p = ((x === friendly.in_w.dst.x) && (y === friendly.in_w.dst.y))
    // val Fp4e = (y != friendly.in_p.dst.y)// planet 2 noc
    // val Fp4s = ((y === friendly.in_p.dst.y) && (x != friendly.in_p.dst.x))
    // val Fp4p = ((y === friendly.in_p.dst.y) && (x === friendly.in_p.dst.x))// I guess this corner case could exist with random destinations

    // //enemy routing
    // val En4s = (x != enemy.in_n.dst.x)//go straight
    // val Ew4e = (y != enemy.in_w.dst.y)
    // val Ew4s = ((y === enemy.in_w.dst.y) && (x != enemy.in_w.dst.x))//turn
    // val En4p = (x === enemy.in_n.dst.x)//exit noc
    // val Ew4p = ((x === enemy.in_w.dst.x) && (y === enemy.in_w.dst.y))
    // val Ep4e = (y != enemy.in_p.dst.y)// planet 2 noc
    // val Ep4s = ((y === enemy.in_p.dst.y) && (x != enemy.in_p.dst.x))
    // val Ep4p = ((y === enemy.in_p.dst.y) && (x === enemy.in_p.dst.x))// I guess this corner case could exist with random destinations

    // //COMBAT
    // friendly_str = (Mux(friendly.in_n.valid, friendly.in_n.fleet_hp, 0.U) + Mux(friendly.in_w.valid, friendly.in_w.fleet_hp, 0.U) + Mux(friendly.in_p.valid, friendly.in_p.fleet_hp, 0.U))
    // enemy_str = (Mux(enemy.in_n.valid, enemy.in_n.fleet_hp, 0.U) + Mux(enemy.in_w.valid, enemy.in_w.fleet_hp, 0.U) + Mux(enemy.in_p.valid, enemy.in_p.fleet_hp, 0.U))
    
    // if((friendly_str != 0) && (enemy_str != 0)){//then we have a fight
    //     if(friendly_str > enemy_str){

    //     }
    // }
}