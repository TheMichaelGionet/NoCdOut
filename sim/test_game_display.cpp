#include <iostream>
#include <stdio.h>

#include "game_display.cpp"

void do_test()
{
    //typedef struct
    //{
    //    int num_players;
    //    int num_generals;
    //    int noc_x_size;
    //    int noc_y_size;
    //    int num_ship_classes;
    //    int max_fleet_hp;
    //    int max_turret_hp;
    //    int max_resource_val;
    //} GameParameters;
    
    GameParameters p;
    p.num_players       = 2;
    p.num_generals      = 4;
    p.noc_x_size        = 4; // make 4 by 4 to make sure it all prints fine, and for my own sanity. 
    p.noc_y_size        = 4;
    p.num_ship_classes  = 6;
    p.max_fleet_hp      = 0xfff;
    p.max_turret_hp     = 0xfff;
    p.max_resource_val  = 0xffff;
    
    Coordinates planet_positions[2];
    planet_positions[0].x = 0;
    planet_positions[0].y = 3;
    planet_positions[1].x = 2;
    planet_positions[1].y = 1;
    
    GameState st( &p, planet_positions, 2 );
    
    int x;
    int y;
    for( x = 0; x < 4; x++ )
    {
        for( y = 0; y < 4; y++ )
        {
            st.space_states[x][y].noc.one_ship                          = false;
            st.space_states[x][y].noc.more_ship                         = false;
            st.space_states[x][y].noc.combat                            = false;
            
            st.space_states[x][y].io.in.bp                              = false;
            st.space_states[x][y].io.in.ship.src.x                      = 0;
            st.space_states[x][y].io.in.ship.src.y                      = 0;
            st.space_states[x][y].io.in.ship.dst.x                      = 0;
            st.space_states[x][y].io.in.ship.dst.y                      = 0;
            st.space_states[x][y].io.in.ship.general_id.side            = 0;
            st.space_states[x][y].io.in.ship.general_id.general_owned   = 0;
            st.space_states[x][y].io.in.ship.ship_class                 = 0;
            st.space_states[x][y].io.in.ship.fleet_hp                   = 0;
            st.space_states[x][y].io.in.ship.scout_data.data_valid      = false;
            st.space_states[x][y].io.in.ship.scout_data.loc.x           = 0;
            st.space_states[x][y].io.in.ship.scout_data.loc.y           = 0;
            st.space_states[x][y].io.in.ship.scout_data.owned           = false;
            st.space_states[x][y].io.in.ship.scout_data.side            = 0;
            st.space_states[x][y].io.in.ship_valid                      = false;
            
            st.space_states[x][y].io.out.bp                             = false;
            st.space_states[x][y].io.out.ship.src.x                     = 0;
            st.space_states[x][y].io.out.ship.src.y                     = 0;
            st.space_states[x][y].io.out.ship.dst.x                     = 0;
            st.space_states[x][y].io.out.ship.dst.y                     = 0;
            st.space_states[x][y].io.out.ship.general_id.side           = 0;
            st.space_states[x][y].io.out.ship.general_id.general_owned  = 0;
            st.space_states[x][y].io.out.ship.ship_class                = 0;
            st.space_states[x][y].io.out.ship.fleet_hp                  = 0;
            st.space_states[x][y].io.out.ship.scout_data.data_valid     = false;
            st.space_states[x][y].io.out.ship.scout_data.loc.x          = 0;
            st.space_states[x][y].io.out.ship.scout_data.loc.y          = 0;
            st.space_states[x][y].io.out.ship.scout_data.owned          = false;
            st.space_states[x][y].io.out.ship.scout_data.side           = 0;
            st.space_states[x][y].io.out.ship_valid                     = false;
        }
    }
    
    st.print_game_state();
    
    st.space_states[0][1].noc.one_ship  = true;
    st.space_states[0][2].noc.more_ship = true;
    st.space_states[0][3].noc.combat    = true;

    st.print_game_state();
    
    st.space_states[0][1].io.in.ship_valid  = true;
    st.space_states[0][2].io.out.ship_valid = true;
    st.space_states[0][3].io.in.ship_valid  = true;
    st.space_states[0][3].io.out.ship_valid = true;
    
    st.space_states[2][3].io.in.ship_valid  = true;
    st.space_states[2][3].io.out.ship_valid = true;
    
    st.print_game_state();
}


int main()
{
    do_test();
    return 0;
}
