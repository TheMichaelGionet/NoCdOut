package noc

import chisel3._
import chisel3.util.Decoupled

import common._

class InPerSide(params: GameParameters) extends Bundle {
    val in_n = new Ship(params)//full bidirectionality!
    val in_w = new Ship(params)
    val in_s = new Ship(params)
    val in_e = new Ship(params)
    //val in_p = new Ship(params)//planet/PE i/o -> maybe change this to be not per side? idk
}

class OutPerSide(params: GameParameters) extends Bundle {
    val out_n = new Ship(params)
    val out_w = new Ship(params)
    val out_s = new Ship(params)
    val out_e = new Ship(params)
    val out_p = new Ship(params)
}

class Router(params: GameParameters, x: UInt, y: UInt) extends Module{
    val io = IO(new Bundle{
        val packets_in = new InPerSide(params)
        val planet_in = new Ship(params)
        val packets_out = new OutPerSide(params)
    })

    // 2 separate DOR routers, w/n -> s/e + s/e -> w/n
    // possible change this to closer to diagonal instead of DOR, but it really shouldn't matter that much
    // intent signals
    val n4s = ((y =/= io.packets_in.in_n.dst.y) && io.packets_in.in_n.valid.asBool) //go straight
    val w4e = ((x =/= io.packets_in.in_w.dst.x) && io.packets_in.in_w.valid.asBool)
    val w4s = (((x === io.packets_in.in_w.dst.x) && (y > io.packets_in.in_w.dst.y)) && io.packets_in.in_w.valid.asBool)//turn
    val n4p = ((x === io.packets_in.in_n.dst.x) && io.packets_in.in_n.valid.asBool)//exit noc
    val w4p = (((x === io.packets_in.in_w.dst.x) && (y === io.packets_in.in_w.dst.y)) && io.packets_in.in_w.valid.asBool)
    val p4e = ((x < io.planet_in.dst.x) && io.planet_in.valid.asBool)// planet to noc, only to N/W -> S/E noc this part
    val p4s = (((x === io.planet_in.dst.x) && (y > io.planet_in.dst.y)) && io.planet_in.valid.asBool)
    val p4p = (((x === io.planet_in.dst.x) && (y === io.planet_in.dst.y)) && io.planet_in.valid.asBool)// I guess this corner case could exist with random destinations

    // conflict resolution
    val n2s = n4s && !io.packets_out.out_s.backpressured // deny packet transfer when output VC is BP'd
    val w2e = w4e && !io.packets_out.out_e.backpressured
    val n2p = n4p // give prio to N exiting NoC
    val w2s = w4s && !n2s && !io.packets_out.out_s.backpressured // w loses to n
    val w2p = w4p && !n2p
    val p2e = p4e && !w2e && !io.packets_out.out_e.backpressured
    val p2s = p4s && !w2s && !n2s && !io.packets_out.out_s.backpressured

    //second set of intents + conflicts
    val s4n = ((y =/= io.packets_in.in_s.dst.y) && io.packets_in.in_s.valid.asBool) //go straight
    val e4w = ((x =/= io.packets_in.in_e.dst.x) && io.packets_in.in_e.valid.asBool)
    val e4n = (((x === io.packets_in.in_e.dst.x) && (y < io.packets_in.in_e.dst.y)) && io.packets_in.in_e.valid.asBool)//turn
    val s4p = ((y === io.packets_in.in_s.dst.y) && io.packets_in.in_s.valid.asBool)//exit noc
    val e4p = (((x === io.packets_in.in_e.dst.x) && (y === io.packets_in.in_e.dst.y)) && io.packets_in.in_e.valid.asBool)
    val p4w = ((x > io.planet_in.dst.x) && io.planet_in.valid.asBool)// planet to noc, only to N/W -> S/E noc this part
    val p4n = (((x === io.planet_in.dst.x) && (y < io.planet_in.dst.y)) && io.planet_in.valid.asBool)
    //val p4p = (((y === packets_in.in_p.dst.y) && (x === packets_in.in_p.dst.x)) planet_in.valid)// I guess this corner case could exist with random destinations

    // conflict resolution
    val s2n = s4n && !io.packets_out.out_n.backpressured // no conflicts here
    val e2w = e4w && !io.packets_out.out_w.backpressured
    val s2p = s4p && !w2p && !n2p // give prio to s exiting NoC, this noc lower prio
    val e2n = e4n && !s2n && !io.packets_out.out_n.backpressured // e loses to s
    val e2p = e4p && !s2p && !n2p && !w2p // this noc lower prio output
    val p2w = p4w && !e2w && !io.packets_out.out_w.backpressured
    val p2n = p4n && !e2n && !s2n && !io.packets_out.out_n.backpressured // simplify this signal for better synth

    // extra turns for horiz-first routing
    val w4n = ((x === io.packets_in.in_w.dst.x) && (y < io.packets_in.in_w.dst.y) && io.packets_in.in_w.valid.asBool)
    val e4s = ((x === io.packets_in.in_e.dst.x) && (y > io.packets_in.in_e.dst.y) && io.packets_in.in_e.valid.asBool)
    // and their resolution
    val w2n = w4n && !s2n && !e2n && !io.packets_out.out_n.backpressured //these turns are lower prio than in-noc turns
    val e2s = e4s && !n2s && !w2s && !io.packets_out.out_s.backpressured
    
    // deprioritize p2p as much as possible
    val p2p = p4p && !n2p && !e2p && !w2p && !s2p

    // write outputs
    when (io.packets_out.out_s.backpressured.asBool) {//to south
        //do nothing
    }.elsewhen (n2s) { 
        io.packets_out.out_s := io.packets_in.in_n
        //io.packets_out.out_s.backpressured := 0.U // remove backpressure from flowing forwards if we have forward movement!
    }.elsewhen (w2s) {
        io.packets_out.out_s := io.packets_in.in_w
        //io.packets_out.out_s.backpressured := 0.U // I will probably need some temp holding thingy for this cause I reckon bad things are going to happen LOL
    }.elsewhen (e2s) {
        io.packets_out.out_s := io.packets_in.in_e
        //io.packets_out.out_s := 0.U // nvm update BP status after combat, surely that'll work, right?
    }.elsewhen (p2s) {
        io.packets_out.out_s := io.planet_in
        //io.packets_out.out_s := 0.U
    }.otherwise {
        io.packets_out.out_s.valid := 0.U //invalid packet if nothing flows to output
    }

    when (io.packets_out.out_n.backpressured.asBool){// to north
        //do nothing
    }.elsewhen (s2n) {
        io.packets_out.out_n := io.packets_in.in_s
        //io.packets_out.out_s.backpressured := 0.U
    }.elsewhen (w2n) {
        io.packets_out.out_n := io.packets_in.in_w
        //io.packets_out.out_n.backpressured := 0.U
    }.elsewhen (e2n) {
        io.packets_out.out_n := io.packets_in.in_e
    }.elsewhen (p2n) {
        io.packets_out.out_n := io.planet_in
    }.otherwise{
        io.packets_out.out_n.valid := 0.U
    }

    when (io.packets_out.out_e.backpressured.asBool){//to east
        //do nothing
    }.elsewhen(w2e){
        io.packets_out.out_e := io.packets_in.in_w
    }.elsewhen(p2e){
        io.packets_out.out_e := io.planet_in
    }.otherwise{
        io.packets_out.out_e.valid := 0.U
    }

    when (io.packets_out.out_w.backpressured.asBool){//to west
        //do nothing
    }.elsewhen(e2w){
        io.packets_out.out_w := io.packets_in.in_e
    }.elsewhen(p2w){
        io.packets_out.out_w := io.planet_in
    }.otherwise{
        io.packets_out.out_w.valid := 0.U
    }

    when(n2p){//to planet, planet accepts ship every cycle so no bp
        io.packets_out.out_p := io.packets_in.in_n
    }.elsewhen(s2p){
        io.packets_out.out_p := io.packets_in.in_s
    }.elsewhen(w2p){
        io.packets_out.out_p := io.packets_in.in_w
    }.elsewhen(e2p){
        io.packets_out.out_p := io.packets_in.in_e
    }.elsewhen(p2p){
        io.packets_out.out_p := io.planet_in
    }.otherwise{
        io.packets_out.out_p.valid := 0.U
    }

    //update BPs in another module so they are applied last, overwriting everything else (?)
}



class NocSwitch(params: GameParameters, x: Int, y: Int) extends Module {
    val io = IO(new Bundle{
        val in = new InPerSide(params) // one physical channel!
        val out = new OutPerSide(params)
        val in_p = new Ship(params) // planet i/o
    })

    //val vcs_in = Seq.fill(params.num_players)(Wire(new InPerSide(params)))
    val vc_in_reg = Reg(Vec(params.num_players, new InPerSide(params))) // I am honestly not sure if this works but sure it does!
    
    for(i <- 0 until params.num_players){//update VC if the side matches iff not backpressured
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
        // when(!vc_in_reg(i).in_p.backpressured && (io.in.in_p.general_id.side === i.U)){
        //     vc_in_reg(i).in_p := io.in.in_p
        // }
        //vc_in_reg(i).elements.map{case (name, data) => vc_in_reg(i).elements(name).valid := (io.in.elements(name).general_id.side === i)}
    }

    val vc_out_reg = Reg(Vec(params.num_players, new OutPerSide(params)))  //output VCs   

    // for ((fieldName, ship) <- io.in.elements){
    //     val targetVC = ship.general_id.side
    //     when(ship.valid && !(vc_in_reg(targetVC)(fieldName).backpressured)){//when we can update VC
    //         vc_in_reg(targetVC).elements(fieldName) := ship
    //     }
    // }

    //shoddy DOR for now
    val vc_routers = Seq[Router]()
    for (i <- 0 until params.num_players){
        vc_routers :+ Module(new Router(params, x.U, y.U))//route per vc. as bp is only valid within vc, otherwise they fight!
        vc_routers(i).io.packets_in := vc_in_reg(i)
        vc_routers(i).io.planet_in := io.in_p // note PE is not registered in/out!
        vc_routers(i).io.planet_in.valid := (io.in_p.general_id.side === i.U)
        vc_routers(i).io.packets_out := vc_out_reg(i)
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