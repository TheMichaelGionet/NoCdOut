NoCdOut: A 4X space simulator on a NoC
=======================

# Setup
## Install Chisel
Follow these instructions https://www.chisel-lang.org/docs/installation

Alternatively, pull this docker container which already has chisel and sbt setup in it: 
```bash
docker pull themichaelgionet/zhw_container2
```
After pulling the container and doing
```bash
docker run -it themichaelgionet/zhw_container2:latest bash
```
Navigate to the "/chipyard/" directory and do
```bash
source env.sh
```
Now the rest can be done.

## Pull this repo
```bash
git clone https://github.com/TheMichaelGionet/NoCdOut.git
```

Congrats, you're setup now.

# Run

## Quickstart:

Navigate to the top most directory and do
```bash
sbt test
```

This should pull anything needed by scala, followed by running the test files. The game's output is generated by one of these test files. It is expected that a grid of ascii text will be drawn. 

Since chisel's default testing environment is being leveraged for simulation at the moment, it is expected to run for a few minutes before producing an output. All of the green text is passed test cases for each of the components.

Each cell in the grid is represented by 4 ascii characters, broken further down into 3 characters to denote what is going on in the PE, and 1 character on the right of those to denote what is going on in the NoC switch. Notation for PEs is as follows:
```
(01
```
Denotes a planet with index 01. 
```
 . 
```
Denotes empty space.
```
{01
```
Still denotes a planet, but it has a ship in the atmosphere trying to leave
```
(> 
```
Is also a planet, but it has a ship in it's garrison (storage).

For empty space, 
```
>. 
```
Denotes that a fleet will begin inspecting it, and
```
 .>
```
Denotes that a fleet is done inspection and wants to go back into the NoC.


Next to each of those is one of:
```
 
```
```
v
```
```
X
```
```
#
```

Which respectively refers to "nothing is being routed", "one fleet is being routed", "multiple fleets are being routed", and "fleets are fighting"

## How to Play:

in the file /NoCdOut/src/main/scala/NoCdOut/level_one.scala, there is a list of general_assignments that looks like:
```scala
val general_map : Map[String, Int] = Map( //assign their locations from 0-4!
    "William" ->        0,
    "Andy" ->           1,
    "Skippy" ->         2,
    "SpaceManMike" ->   3,
    "Oobleck" ->        4
)
```
Change the number next to each general from 0 to 4 to assign them to different planets. Once that is done, run this in the topmost directory /NoCdOut/ to see how the game plays out:

```bash
sbt test
```
Install SBT from https://www.scala-sbt.org/ if you don't have it.

A high level description of each general is given below. 


# Infoz

## How It Works:

### The NoC:
The NoC topology is a bidirectional mesh, which was chosen because it can best represent space. 
- Within the NoC, packets arrive and are registered into input registers per virtual channel (which is per team) and are then routed per team using horizontal-first routing to ensure deadlock free routing on a mesh.
- Upon completion, all VCs that are competing for an output are subjected to combat, in which the strongest ship wins and is damaged according to the strength of the second strongest ship. Complete destruction is very possible.
- After those fights have been resolved, they are mashed into a global fight across the whole switch, in which the same process occurs, but this time there can be multiple ships from the same side competing, so logic must exist here to sum up strengths and spread out the damage fairly.
- We chose to have a router per VC instead of a global combat per side as it seemed cheaper in the long run as the routers are fairly clean mux/LUT-blobs whereas the global combat has a lot of costly operations that seemed worth not using. Also, when the team size is small, it is a small number of routers instead of an additional combat unit per side (a fixed number).
- In relation to the construction of the switch, only the input and output were pipelined in the hopes of minimizing the number of execution cycles of simulation and maximizing the number of routed packets per cycle (destroyed spaceships are free real estate!). In deployment to FPGA, length of the critical path within the switch would be investigated and pipelined if deemed to be an area of performance loss.
- Some general goals for the NoC include minimizing packets in flight to speed up arrival times and throughput of the NoC by having more - combat and always prioritizing packets leaving the NoC over entering the NoC.


Diagrams can be found in the /documentation/ subdirectory. 

### Planets/PEs:
Every planet has 3 main things:
- Resources
- Turrets
- A General

A General will take information that is available to them, like how many resources the planet has, how healthy the turrets are, what fleet entered the planet's atmosphere, and any information retreived by scouts. 

The General will use that information to make a decision about whether or not to buy a new fleet, which fleet type, where to route fleets, or when and by how much to strengthen the planet's turret. 

In the code, a General just outputs those decisions, and the relevant handler will make it happen. A planet as a module has the following submodules:
- ShipEventHandler: Given a fleet that was routed to the planet, reroute to where the general decides given that the general owns it, routes it back to the source if it is friendly or an enemy scout, or decide to damage the planet if it's hostile.
- CombatHandler: If the fleet is hostile (and not a scout), compute how much damage is done to the planet's turret and if it's enough to take over the planet. Factor in turret hp regeneration.
- EconomyHandler: If a General wants to buy a fleet or strengthen turret defences, compute whether or not it can do this and what the resources (==space dollars) are left afterwards.
- GeneralMux: This just multiplexes the controls between the friendly General, enemy General, or no General based on who owned the planet.


Empty Space is also a module that occupies wherever a planet does not. The logic for these is much simpler and amounts to "any ship that gets routed here will just be routed back where it came from".

### Ship Classes:
Ships are divided into:
- Scouts (have the special property that they can retrieve information and bring it back to a general)
- Basic (baseline attack/defence, cheap)
- Attack (higher damage per hit point ratio)
- Defence (lower damage but high hp)
- Beefer (balanced between attack and defence)
- Destroyer (high attack and high defence, expensive)

### Some Generals:
To give a high level description of each general:
- Oobleck will either focus on exploration and exploitation or combat and defence at any given time
- Bush Man Mike is a wildcard that prefers exploiting resources and defence
- Space Man Mike prefers to exploration and exploitation over combat
- Da Borg prefers combat
- William prefers defence but when engaged is brutal
- Onion prefers to try to conquor new planets and fortifying its own defences
- Greg prefers pure combat
- Skippy does not do combat, but does create a cloud of defensive ships around his planet

## Work Division (for ECE720)

### Michael:
- Generals:
    - General Abstract Module
    - Parametrized General
    - A few generals that instantiate the parametrized general
- Planets:
    - Ship Event Handler
    - Combat Handler (the planet side version)
    - Economy Handler
- Level Builder Module
- Level Builder + NoC Integration

### Ryan:
- Made the Parametrized NoC:
    - Router
    - Combat Units
    - Switch with Backpressure propogation
    - Noc Instantiator
- Made level one
- Was(is) deathly ill and hard carried by Michael


# Disclaimer:
This is a work of fiction. Names, characters, events, and incidents are purely the products of the developers' imagination. Any resemblance to actual persons, living or dead, or actual events is purely coincidental.



