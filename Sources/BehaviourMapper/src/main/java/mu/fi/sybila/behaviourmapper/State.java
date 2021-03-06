package mu.fi.sybila.behaviourmapper;

import java.util.Arrays;

/**
 * Class representing a state in the state transition graph of the model.
 * 
 * @author George Kolcak
 */
public class State
{
    
    private int[] activityLevels;
    private int buchiAutomatonState;
    
    /**
     * Constructor decoding the state from a string.
     * 
     * @param values The string encoding the state.
     *               The string should be a comma separated values of individual specie activity levels.
     */
    public State(String values)
    {
        String[] data = values.split(";");
        
        buchiAutomatonState = Integer.parseInt(data[1]);
        
        String[] levels = data[0].split(",");
        
        activityLevels = new int[levels.length];
        for (int i = 0; i < levels.length; i++)
        {
            activityLevels[i] = Integer.parseInt(levels[i]);
        }
    }
    
    public int[] getActivityLevels()
    {
        return activityLevels;
    }
    
    public int getBuchiAutomatonState()
    {
        return buchiAutomatonState;
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        s.append('(');
        
        for (int i = 0; i < activityLevels.length; i++)
        {
            if (i > 0)
            {
                s.append(',');
            }
                
            s.append(activityLevels[i]);
        }
        
        s.append(';');
        s.append(buchiAutomatonState);
        s.append(')');
        
        return s.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if ((obj == null) || (getClass() != obj.getClass()))
        {
            return false;
        }
        
        final State other = (State) obj;
        
        if (!Arrays.equals(this.activityLevels, other.activityLevels) ||
                (this.buchiAutomatonState != other.buchiAutomatonState))
        {
            return false;
        }
        
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = ((67 * hash) + Arrays.hashCode(activityLevels));
        hash = ((67 * hash) + buchiAutomatonState);
        return hash;
    }
    
}
