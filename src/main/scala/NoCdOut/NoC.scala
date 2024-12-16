package noc

import chisel3._
import chisel3.util.Decoupled
import chisel3.experimental.BundleLiterals._
import chisel3.util._

import common._

class InPerSide(params: GameParameters) extends Bundle {
    val in_n = Flipped(Decoupled(new Ship(params)))//full bidirectionality!
    val in_w = Flipped(Decoupled(new Ship(params)))
    val in_s = Flipped(Decoupled(new Ship(params)))
    val in_e = Flipped(Decoupled(new Ship(params)))
    //val in_p = new Ship(params)//planet/PE i/o -> maybe change this to be not per side? idk
}

class OutPerSide(params: GameParameters) extends Bundle {
    val out_n = Decoupled(new Ship(params))
    val out_w = Decoupled(new Ship(params))
    val out_s = Decoupled(new Ship(params))
    val out_e = Decoupled(new Ship(params))
    val out_p = Decoupled(new Ship(params))
}

class Router(params: GameParameters, x: UInt, y: UInt) extends Module{
    val io = IO(new Bundle{
        val packets_in = new InPerSide(params)
        val planet_in = Flipped(Decoupled(new Ship(params)))
        val packets_out = new OutPerSide(params)
    })

    // 2 separate DOR routers, w/n -> s/e + s/e -> w/n
    // possible change this to closer to diagonal instead of DOR, but it really shouldn't matter that much
    // intent signals
    val n4s = ((y =/= io.packets_in.in_n.bits.dst.y) && io.packets_in.in_n.valid.asBool) //go straight
    val w4e = ((x =/= io.packets_in.in_w.bits.dst.x) && io.packets_in.in_w.valid.asBool)
    val w4s = (((x === io.packets_in.in_w.bits.dst.x) && (y > io.packets_in.in_w.bits.dst.y)) && io.packets_in.in_w.valid.asBool)//turn
    val n4p = ((x === io.packets_in.in_n.bits.dst.x) && io.packets_in.in_n.valid.asBool)//exit noc
    val w4p = (((x === io.packets_in.in_w.bits.dst.x) && (y === io.packets_in.in_w.bits.dst.y)) && io.packets_in.in_w.valid.asBool)
    val p4e = ((x < io.planet_in.bits.dst.x) && io.planet_in.valid.asBool)// planet to noc, only to N/W -> S/E noc this part
    val p4s = (((x === io.planet_in.bits.dst.x) && (y > io.planet_in.bits.dst.y)) && io.planet_in.valid.asBool)
    val p4p = (((x === io.planet_in.bits.dst.x) && (y === io.planet_in.bits.dst.y)) && io.planet_in.valid.asBool)// I guess this corner case could exist with random destinations

    // conflict resolution
    val n2s = n4s && io.packets_out.out_s.ready // deny packet transfer when output VC is BP'd
    val w2e = w4e && io.packets_out.out_e.ready
    val n2p = n4p // give prio to N exiting NoC
    val w2s = w4s && !n2s && io.packets_out.out_s.ready // w loses to n
    val w2p = w4p && !n2p
    val p2e = p4e && !w2e && io.packets_out.out_e.ready

    //second set of intents + conflicts
    val s4n = ((y =/= io.packets_in.in_s.bits.dst.y) && io.packets_in.in_s.valid.asBool) //go straight
    val e4w = ((x =/= io.packets_in.in_e.bits.dst.x) && io.packets_in.in_e.valid.asBool)
    val e4n = (((x === io.packets_in.in_e.bits.dst.x) && (y < io.packets_in.in_e.bits.dst.y)) && io.packets_in.in_e.valid.asBool)//turn
    val s4p = ((y === io.packets_in.in_s.bits.dst.y) && io.packets_in.in_s.valid.asBool)//exit noc
    val e4p = (((x === io.packets_in.in_e.bits.dst.x) && (y === io.packets_in.in_e.bits.dst.y)) && io.packets_in.in_e.valid.asBool)
    val p4w = ((x > io.planet_in.bits.dst.x) && io.planet_in.valid.asBool)// planet to noc, only to N/W -> S/E noc this part
    val p4n = (((x === io.planet_in.bits.dst.x) && (y < io.planet_in.bits.dst.y)) && io.planet_in.valid.asBool)
    //val p4p = (((y === packets_in.in_p.dst.y) && (x === packets_in.in_p.dst.x)) planet_in.valid)// I guess this corner case could exist with random destinations

    // conflict resolution
    val s2n = s4n && io.packets_out.out_n.ready // no conflicts here
    val e2w = e4w && io.packets_out.out_w.ready
    val s2p = s4p && !w2p && !n2p // give prio to s exiting NoC, this noc lower prio
    val e2n = e4n && !s2n && io.packets_out.out_n.ready // e loses to s
    val e2p = e4p && !s2p && !n2p && !w2p // this noc lower prio output
    val p2w = p4w && !e2w && io.packets_out.out_w.ready

    // extra turns for horiz-first routing
    val w4n = ((x === io.packets_in.in_w.bits.dst.x) && (y < io.packets_in.in_w.bits.dst.y) && io.packets_in.in_w.valid.asBool)
    val e4s = ((x === io.packets_in.in_e.bits.dst.x) && (y > io.packets_in.in_e.bits.dst.y) && io.packets_in.in_e.valid.asBool)
    // and their resolution
    val w2n = w4n && !s2n && !e2n && io.packets_out.out_n.ready //these turns are lower prio than in-noc turns
    val e2s = e4s && !n2s && !w2s && io.packets_out.out_s.ready

    val p2s = p4s && !w2s && !n2s && io.packets_out.out_s.ready && !e2s // planets injecting onto NoC has lowest priority
    val p2n = p4n && !e2n && !s2n && io.packets_out.out_n.ready && !w2n // simplify this signal for better synth
    
    // deprioritize p2p as much as possible
    val p2p = p4p && !n2p && !e2p && !w2p && !s2p

    val out_s_wires = Wire(new Ship(params))
    out_s_wires := DontCare
    // val out_s_val_shreg = RegInit(0.U(1.W)) //include a shadow register for ready on output to deal with bp
    // write outputs
    when (!io.packets_out.out_s.ready.asBool) {//to south
        io.packets_out.out_s.valid := DontCare 
    }.elsewhen (n2s) { 
        out_s_wires := io.packets_in.in_n.bits
        io.packets_out.out_s.valid := 1.U
        // out_s_val_shreg := 1.U
    }.elsewhen (w2s) {
        out_s_wires := io.packets_in.in_w.bits
        io.packets_out.out_s.valid := 1.U
        // out_s_val_shreg := 1.U
    }.elsewhen (e2s) {
        out_s_wires := io.packets_in.in_e.bits
        io.packets_out.out_s.valid := 1.U
        // out_s_val_shreg := 1.U
    }.elsewhen (p2s) {
        out_s_wires := io.planet_in.bits
        io.packets_out.out_s.valid := 1.U
        // out_s_val_shreg := 1.U
    }.otherwise {
        //io.packets_out.out_s.bits := DontCare
        io.packets_out.out_s.valid := 0.U //invalid packet if nothing flows to output
        // out_s_val_shreg := 0.U
    }
    io.packets_out.out_s.bits := out_s_wires


    val out_n_wires = Wire(new Ship(params))
    out_n_wires := DontCare
    // val out_n_val_shreg = RegInit(0.U(1.W))
    when (!io.packets_out.out_n.ready.asBool){// to north
        io.packets_out.out_n.valid := DontCare
    }.elsewhen (s2n) {
        out_n_wires := io.packets_in.in_s.bits
        io.packets_out.out_n.valid := 1.U
        // out_n_val_shreg := 1.U
    }.elsewhen (w2n) {
        out_n_wires := io.packets_in.in_w.bits
        io.packets_out.out_n.valid := 1.U
        // out_n_val_shreg := 1.U
    }.elsewhen (e2n) {
        out_n_wires := io.packets_in.in_e.bits
        io.packets_out.out_n.valid := 1.U
        //out_n_val_shreg := 1.U
    }.elsewhen (p2n) {
        out_n_wires := io.planet_in.bits
        io.packets_out.out_n.valid := 1.U
        //out_n_val_shreg := 1.U
    }.otherwise{
        //io.packets_out.out_n.bits := DontCare
        io.packets_out.out_n.valid := 0.U
        //out_n_val_shreg := 0.U
    }
    io.packets_out.out_n.bits := out_n_wires

    val out_e_wires = Wire(new Ship(params))
    out_e_wires := DontCare
    //val out_e_val_shreg = RegInit(0.U(1.W))
    when (!io.packets_out.out_e.ready.asBool){//to east
        io.packets_out.out_e.valid := DontCare
    }.elsewhen(w2e){
        out_e_wires := io.packets_in.in_w.bits
        io.packets_out.out_e.valid := 1.U
        //out_e_val_shreg := 1.U
    }.elsewhen(p2e){
        out_e_wires := io.planet_in.bits
        io.packets_out.out_e.valid := 1.U
        //out_e_val_shreg := 1.U
    }.otherwise{
        io.packets_out.out_e.valid := 0.U
        //out_e_val_shreg := 0.U
    }
    io.packets_out.out_e.bits := out_e_wires

    val out_w_wires = Wire(new Ship(params))
    out_w_wires := DontCare
    // val out_w_val_shreg = RegInit(0.U(1.W))
    when (!io.packets_out.out_w.ready.asBool){//to west
        io.packets_out.out_w.valid := DontCare
    }.elsewhen(e2w){
        out_w_wires := io.packets_in.in_e.bits
        io.packets_out.out_w.valid := 1.U
        // out_w_val_shreg := 1.U
    }.elsewhen(p2w){
        out_w_wires := io.planet_in.bits
        io.packets_out.out_w.valid := 1.U
        // out_w_val_shreg := 1.U
    }.otherwise{
        io.packets_out.out_w.bits := DontCare
        io.packets_out.out_w.valid := 0.U
        // out_w_val_shreg := 1.U
    }
    io.packets_out.out_w.bits := out_w_wires

    val out_p_wires = Wire(new Ship(params))
    out_p_wires := DontCare
    when(!io.packets_out.out_p.ready){
        io.packets_out.out_p.valid := DontCare
    }.elsewhen(n2p){//to planet, planet accepts ship every cycle so no bp
        out_p_wires := io.packets_in.in_n.bits
        io.packets_out.out_p.valid := 1.U
    }.elsewhen(s2p){
        out_p_wires := io.packets_in.in_s.bits
        io.packets_out.out_p.valid := 1.U
    }.elsewhen(w2p){
        out_p_wires := io.packets_in.in_w.bits
        io.packets_out.out_p.valid := 1.U
    }.elsewhen(e2p){
        out_p_wires := io.packets_in.in_e.bits
        io.packets_out.out_p.valid := 1.U
    }.elsewhen(p2p){
        out_p_wires := io.planet_in.bits
        io.packets_out.out_p.valid := 1.U
    }.otherwise{
        // io.packets_out.out_p.bits := io.planet_in.bits
        io.packets_out.out_p.valid := 0.U
    }
    io.packets_out.out_p.bits := out_p_wires

    //backpressure propagation
    when((n4s && !n2s) || (n4p && !n2s)){//north
        io.packets_in.in_n.ready := 0.U
   }.otherwise{
        io.packets_in.in_n.ready := 1.U
   }

   when((s4n && !s2n) || (s4p && !s2p)){//south
        io.packets_in.in_s.ready := 0.U
   }.otherwise{
        io.packets_in.in_s.ready := 1.U
   }

   when((w4e && !w2e) || (w4n && !w2n) || (w4s && !w2s) || (w4p && !w2p)){//west
        io.packets_in.in_w.ready := 0.U
   }.otherwise{
        io.packets_in.in_w.ready := 1.U
   }

   when((e4w && !e2w) || (e4n && !e2n) || (e4s && !e2s) || (e4p && !e2p)){//east
        io.packets_in.in_e.ready := 0.U
   }.otherwise{
        io.packets_in.in_e.ready := 1.U
   }

   when((p4e && !p2e) || (p4w && !p2w) || (p4n && !p2n) || (p4s && !p2s) || (p4p && !p2p)){//planet but also this one is tricky, as all routers see the same(?) planet_in data
        io.planet_in.ready := 0.U//this case is obvious that it is the driver
   }.otherwise{
        io.planet_in.ready := 1.U
   }

}

// class DisgustingMaxBundle(params: GameParameters) extends Bundle{
//     val max_val = UInt(params.max_fleet_hp_len.W)
//     val next_val = UInt(params.max_fleet_hp_len.W)
// }

class FightPerSide(params: GameParameters) extends Module {
    val io = IO(new Bundle{
        val ins = Vec(params.num_players, Flipped(Decoupled(new Ship(params))))
        val out = Decoupled(new Ship(params)) // consider changing this over to just the bits + valids, idk if we need readys if no regs or blocking logic
    })

    // commit combat between all sides >:D
    val strengths = io.ins.map((ship: DecoupledIO[Ship]) => Mux(ship.valid, ship.bits.fleet_hp, 0.U)) // collect valid strengths
    val max_strength = strengths.reduce((a,b) => Mux(a > b, a, b)) // find the largest value
    val second_max_strength = strengths.reduce((a,b) => Mux(a === max_strength, Mux(b === max_strength, 0.U, b), Mux(b === max_strength, a, Mux(a > b, a, b))))

    val who_is_max = VecInit(strengths.map(_ === max_strength)) // there can be ties
    val max_index = PriorityEncoder(who_is_max) // do this instead of OHtoUInt as we might have ties, idk if this makes an issue, maybe chisel has an internal assertion I didn't check.

    // val rand = LFSR(params.num_players)
    // val rand_max = who_is_max.zipWithIndex.map { case (isMax, idx) => rand(idx) && isMax}
    // val maybe_one_hot = Mux(rand_max.reduce(_ || _), rand_max, who_is_max) //could randomly say nobody wins LOL
    // val max_index = PriorityEncoder(maybe_one_hot) // prio encode remaining ties to a 1H index

    val MAD = (PopCount(who_is_max) =/= 1.U) //if more than 1 max value (or no valid ships), everything blows each other up :)

    // val max_bundle = new DisgustingMaxBundle(params)
    // max_bundle = strengths.foldLeft[DisgustingMaxBundle](new DisgustingMaxBundle(params).Lit(_.max_val -> 0.U(params.max_fleet_hp_len.W), _.next_val -> 0.U(params.max_fleet_hp_len.W))) 
    //     { case ((max_bundle.max_val, max_bundle.next_val), value) =>
    //     Mux(value > max_bundle.max_val, (value, max_bundle.max_val), Mux(value > max_bundle.next_val, (max_bundle.max_val, value), (max_bundle.max_val, max_bundle.next_val)))} // get the second largest value in a disgusting mux chain T_T

    when (MAD) { //if we blow everything up, nothing is valid LOL
        io.out.bits := DontCare 
        io.out.valid := false.B 
    }.otherwise {
        io.out.bits := io.ins(max_index).bits
        io.out.bits.fleet_hp := max_strength - second_max_strength // adjusted HP post fight
        io.out.valid := true.B
    }
    
    //the idea is to take the strongest ship and throw everything else against it
    //guaranteed to only have one ship per side here
}


class NocSwitch(params: GameParameters, x: Int, y: Int) extends Module {
    val io = IO(new Bundle{
        val in = new InPerSide(params) // one physical channel!
        val out = new OutPerSide(params)
        val in_p = Flipped(Decoupled(new Ship(params))) // planet i/o
    })

    //val vcs_in = Seq.fill(params.num_players)(Wire(new InPerSide(params)))
    val vc_in_reg = Reg(Vec(params.num_players, new InPerSide(params))) // I am honestly not sure if this works but sure it does!
    // TODO: port all these over to regEnables
    
    for(i <- 0 until params.num_players){//update VC if the side matches iff not backpressured
        when(vc_in_reg(i).in_n.ready && (io.in.in_n.bits.general_id.side === i.U)){
            vc_in_reg(i).in_n <> io.in.in_n
        }
        when(vc_in_reg(i).in_w.ready && (io.in.in_w.bits.general_id.side === i.U)){
            vc_in_reg(i).in_w <> io.in.in_w
        }
        when(vc_in_reg(i).in_e.ready && (io.in.in_e.bits.general_id.side === i.U)){
            vc_in_reg(i).in_e <> io.in.in_e
        }
        when(vc_in_reg(i).in_s.ready && (io.in.in_s.bits.general_id.side === i.U)){
            vc_in_reg(i).in_s <> io.in.in_s
        }
    }

    val vc_out_wire = Wire(Vec(params.num_players, new OutPerSide(params)))  //output VC must be RegEnables, change the Vec Wrapper but use wires for now because I am LAZY
    val planet_in_ready_fanout = Wire(Vec(params.num_players, UInt(1.W)))  //fanout for planet ready signals

    val vc_routers = Seq[Router]()
    for (i <- 0 until params.num_players){
        vc_routers :+ Module(new Router(params, x.U, y.U))//route per vc. as bp is only valid within vc, otherwise they fight!
        vc_routers(i).io.packets_in <> vc_in_reg(i)
        vc_routers(i).io.planet_in.bits := io.in_p.bits // note PE is not registered in!
        vc_routers(i).io.planet_in.valid := ((io.in_p.bits.general_id.side === i.U) && io.in_p.valid)
        vc_routers(i).io.planet_in.ready := planet_in_ready_fanout(i)
        vc_routers(i).io.packets_out <> vc_out_wire(i)
    }
    io.in_p.ready := planet_in_ready_fanout.toSeq.reduce(_ & _) //if ANY readys are set to 0, then planet is not ready (a packet is held in the buffer)

    val vc_out_reg = RegInit(Vec(params.num_players, new OutPerSide(params)), Vec(params.num_players, 
        new OutPerSide(params).Lit(
            _.out_n.bits -> DontCare,
            _.out_s.bits -> DontCare,
            _.out_w.bits -> DontCare,
            _.out_e.bits -> DontCare,
            _.out_p.bits -> DontCare,
            _.out_n.ready -> true.B ,
            _.out_s.ready -> true.B,
            _.out_e.ready -> true.B,
            _.out_w.ready -> true.B,
            _.out_p.ready -> true.B,
            _.out_n.valid -> false.B,
            _.out_s.valid -> false.B,
            _.out_e.valid -> false.B,
            _.out_w.valid -> false.B,
            _.out_p.valid -> false.B
            ))) // init to a safe null state
    
    //first we fight per side, then per switch

}