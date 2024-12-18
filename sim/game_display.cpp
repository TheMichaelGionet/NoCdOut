
#include <iostream>
#include <stdio.h>

typedef struct
{
    int num_players;
    int num_generals;
    int noc_x_size;
    int noc_y_size;
    int num_ship_classes;
    int max_fleet_hp;
    int max_turret_hp;
    int max_resource_val;
} GameParameters;

typedef struct
{
    int x;
    int y;
} Coordinates;

typedef struct
{
    int side;
    int general_owned;
} GeneralID;

typedef struct
{
    bool data_valid;
    Coordinates loc;
    bool owned;
    int side;
} ScoutData;

typedef struct
{
    Coordinates src;
    Coordinates dst;
    GeneralID general_id;
    int ship_class;
    int fleet_hp;
    ScoutData scout_data;
} Ship;

typedef struct
{
    bool is_owned;
    int owned_by;
    
    int resources;
    int limit_resources;
    int resource_prod;
    
    int turret_hp;
    
    Ship ship_garrison;
    bool garrison_valid;
} PlanetState;

typedef struct
{
    struct
    {
        bool bp;
        Ship ship;
        bool ship_valid;
    } in;
    
    struct
    {
        bool bp;
        Ship ship;
        bool ship_valid;
    } out;
} SpaceIO;

typedef struct
{
    bool is_planet;
    int planet_index;
    PlanetState * planet; // planets are deleted externally, so don't worry about this.
} ProtoCell;

typedef struct
{
    bool one_ship;
    bool more_ship;
    bool combat;
} NoCState;

typedef struct
{
    SpaceIO io;
    NoCState noc;
    ProtoCell meta;
} SpaceObservable;



class GameState
{
// TODO: make private once done debugging. 
public:
    GameParameters params;
    SpaceObservable ** space_states;
    PlanetState * planet_states;
    int num_planets;
    
    void print_noc_state_character( int x, int y )
    {
        if( space_states[x][y].noc.combat )
        {
            printf( "#" );
        }
        else if( space_states[x][y].noc.more_ship )
        {
            printf( "%%" );
        }
        else if( space_states[x][y].noc.one_ship )
        {
            printf( ">" );
        }
        else
        {
            printf( " " );
        }
    }
    // row = y coordinate
    void print_space_row( int row )
    {
        int x;
        
        for( x = 0; x < params.noc_x_size; x++ )
        {
            if( space_states[x][row].meta.is_planet )
            {
                if( space_states[x][row].io.in.ship_valid || space_states[x][row].io.out.ship_valid )
                {
                    printf( "{" );
                }
                else
                {
                    printf( "(" );
                }

                
                if( space_states[x][row].meta.planet->garrison_valid )
                {
                    printf( "> " );
                }
                else
                {
                    int last_digit = ( space_states[x][row].meta.planet_index & 0xf );
                    int first_digit = ( (space_states[x][row].meta.planet_index>>4) & 0xf );
                    printf( "%x%x", first_digit, last_digit );
                }
            }
            else // if empty space
            {
                if( space_states[x][row].io.in.ship_valid )
                {
                    printf( ">" );
                }
                else
                {
                    printf( " " );
                }

                if( space_states[x][row].io.out.ship_valid )
                {
                    printf( ".>" );
                }
                else
                {
                    printf( ". " );
                }
            }
            print_noc_state_character( x, row );
        }
        printf( "\n" );
    }
    
    void print_space()
    {
        int y;
        for( y = 0; y < params.noc_y_size; y++ )
        {
            print_space_row( y );
        }
    }
    
    void print_planet_states()
    {
        int p;
        printf( "id  owned?  owner   resources   health  ship built\n" );
        for( p = 0; p < num_planets; p++ )
        {
            printf( "%x\t%x\t%x\t%d/%d\t%d\t%x\n", p, planet_states[p].is_owned, planet_states[p].owned_by, planet_states[p].resources, planet_states[p].limit_resources, planet_states[p].turret_hp, planet_states[p].garrison_valid );
        }
    }
public:
    GameState( GameParameters * g, Coordinates * planet_positions, int num_planet_positions )
    {
        params.num_players      = g->num_players;
        params.num_generals     = g->num_generals;
        params.noc_x_size       = g->noc_x_size;
        params.noc_y_size       = g->noc_y_size;
        params.num_ship_classes = g->num_ship_classes;
        params.max_fleet_hp     = g->max_fleet_hp;
        params.max_turret_hp    = g->max_turret_hp;
        params.max_resource_val = g->max_resource_val;
        
        num_planets = num_planet_positions;
        
        int p_iter;
        int x_iter;
        int y_iter;
        
        planet_states = new PlanetState[num_planet_positions];
        
        
        space_states = new SpaceObservable*[params.noc_x_size];
        
        for( x_iter = 0; x_iter < params.noc_x_size; x_iter++ )
        {
            space_states[x_iter] = new SpaceObservable[params.noc_y_size];
            for( y_iter = 0; y_iter < params.noc_y_size; y_iter++ )
            {
                space_states[x_iter][y_iter].meta.is_planet = false;
            }
        }
        
        for( p_iter = 0; p_iter < num_planet_positions; p_iter++ )
        {
            Coordinates c                               = planet_positions[p_iter];
            space_states[c.x][c.y].meta.planet          = &planet_states[p_iter];
            space_states[c.x][c.y].meta.is_planet       = true;
            space_states[c.x][c.y].meta.planet_index    = p_iter;
        }
    }
    
    void print_game_state()
    {
        print_planet_states();
        print_space();
    }
    
    ~GameState()
    {
        delete[] planet_states;
        planet_states = nullptr;
        int x_iter;
        for( x_iter = 0; x_iter < params.noc_x_size; x_iter++ )
        {
            delete[] space_states[x_iter];
            space_states[x_iter] = nullptr;
        }
        delete[] space_states;
        space_states = nullptr;
    }
};
