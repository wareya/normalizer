/*
This software is dual-licensed to the public domain and under the following
license: you are granted a perpetual, irrevocable license to copy, modify,
publish, and distribute this file as you see fit.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.
 */

import com.sun.deploy.util.ArrayUtil;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

public class Main
{
    static boolean simple_average = false;
    static boolean cropped_average = true;
    static boolean more_cropped_average = true;
    static boolean median = false;
    
    static int identity_length = 11;
    
    static private class Fact
    {
        Integer count;
        String identity;
        Fact(Integer count, String identity)
        {
            this.count = count;
            this.identity = identity;
        }
    }
    
    static private class Entry
    {
        Double count;
        String identity;
        HashMap<String, Integer> spellings;
        Entry(Double count, String identity)
        {
            this.count = count;
            this.identity = identity;
            this.spellings = new HashMap<>();
        }
        String spellingsAsString()
        {
            ArrayList<Fact> sorted_spellings = new ArrayList<>();
            for(HashMap.Entry<String, Integer> spelling : spellings.entrySet())
                sorted_spellings.add(new Fact(spelling.getValue(), spelling.getKey()));
                
            sorted_spellings.sort((a, b) -> (b.count - a.count > 0)?1:(b.count - a.count < 0)?-1:0);
            
            String r = "";
            for(Fact spelling : sorted_spellings)
                r += spelling.identity + "\t" + spelling.count + "\t";
            
            return r;
        }
    }
    public static void main(String[] args)
    {
        ArrayDeque<String> arguments = new ArrayDeque<>();
        arguments.addAll(Arrays.asList(args));
        if(arguments.size() == 0)
            GUIMain.main(args);
        else
            ConsoleMain.main(args);
    }
    static private ArrayList<Entry> merge(ArrayList<ArrayList<Entry>> collection)
    {
        TreeMap<String, Double> aggregation = new TreeMap<>();
        TreeMap<String, HashMap<String, Integer>> aggregation2 = new TreeMap<>();
        
        if(simple_average || (!cropped_average && !median && !more_cropped_average))
        {
            Double total_tokens = 0.0;
            for(ArrayList<Entry> list : collection)
            {
                for(Entry entry : list)
                {
                    total_tokens += entry.count;
                    double count = entry.count;
                    if(aggregation.containsKey(entry.identity))
                        aggregation.replace(entry.identity, aggregation.get(entry.identity)+count);
                    else
                        aggregation.put(entry.identity, count);
                }
            }
            for(String key : aggregation.descendingKeySet())
                aggregation.replace(key, aggregation.get(key)*1000000/total_tokens);
        }
        else
        {
            HashMap<String, ArrayList<Double>> terms = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Double>>> spellings = new HashMap<>();
            for(ArrayList<Entry> list : collection)
            {
                for(Entry entry : list)
                {
                    ArrayList<Double> temp;
                    if(!terms.containsKey(entry.identity))
                        temp = new ArrayList<>();
                    else
                        temp = terms.get(entry.identity);
                    temp.add(entry.count);
                    terms.put(entry.identity, temp);
                }
            }
            Double total_tokens = 0.0;
            for(HashMap.Entry<String, ArrayList<Double>> entry : terms.entrySet())
            {
                ArrayList<Double> counts = entry.getValue();
                while(counts.size() < collection.size())
                    counts.add(0.0);
                counts.sort((a,b)->a>b?1:a<b?-1:0);
                if(cropped_average)
                {
                    if(counts.size() >= 3)
                    {
                        counts.remove(0);
                        counts.remove(counts.size()-1);
                    }
                    
                    Double tokens = 0.0;
                    for(Double d : counts)
                        tokens += d;
                    tokens /= counts.size();
                    
                    aggregation.put(entry.getKey(), tokens);
                    total_tokens += tokens;
                }
                else if (more_cropped_average)
                {
                    int target_size = counts.size()/2;
                    if(target_size < 2) target_size = 2;
                    
                    while(counts.size() > target_size)
                    {
                        counts.remove(0);
                        counts.remove(counts.size()-1);
                    }
                    
                    Double tokens = 0.0;
                    for(Double d : counts)
                        tokens += d;
                    tokens /= counts.size();
                    
                    aggregation.put(entry.getKey(), tokens);
                    total_tokens += tokens;
                }
                else
                {
                    Double tokens;
                    if(counts.size()%2 == 1)
                        tokens = counts.get(counts.size()/2);
                    else
                        tokens = (counts.get(counts.size()/2)+counts.get(counts.size()/2-1))/2;
                    aggregation.put(entry.getKey(), tokens);
                    total_tokens += tokens;
                }
                
            }
            for(String key : aggregation.descendingKeySet())
                aggregation.replace(key, aggregation.get(key)*1000000/total_tokens);
        }
        
        for(ArrayList<Entry> list : collection)
        {
            for(Entry entry : list)
            {
                if(aggregation2.containsKey(entry.identity))
                {
                    HashMap<String, Integer> my_word = aggregation2.get(entry.identity);
                    for(HashMap.Entry<String, Integer> spelling : entry.spellings.entrySet())
                    {
                        if(my_word.containsKey(spelling.getKey()))
                            my_word.replace(spelling.getKey(), my_word.get(spelling.getKey()) + spelling.getValue()); 
                    }
                }
                else
                {
                    HashMap<String, Integer> my_word = new HashMap<>(entry.spellings);
                    aggregation2.put(entry.identity, my_word);
                }
            } 
        }
        
        ArrayList<Entry> ret = new ArrayList<>(); 
        
        for(String key : aggregation.descendingKeySet())
        {
            Entry new_entry = new Entry(aggregation.get(key), key);
            new_entry.spellings = new HashMap<>(aggregation2.get(key));
            ret.add(new_entry);
        }
        
        return ret;
    }
    static void run(ArrayList<String> input_names, BufferedWriter out, BiConsumer<String, Double> update) {
        ArrayList<ArrayList<Entry>> collection = new ArrayList<>();
        
        update.accept("Processing inputs files...", -1.0);
        
        for(String filename : input_names)
        {
            ArrayList<Entry> list = new ArrayList<>();
            Scanner file;
            try
            {
                file = new Scanner(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
                file.useDelimiter("\n");
            }
            catch (FileNotFoundException e)
            {
                update.accept("Failed to open an input file.", 0.0);
                return;
            }
            catch (UnsupportedEncodingException e)
            {
                update.accept("Failed to open an input file as UTF-8.", 0.0);
                return;
            }
            
            while(file.hasNext())
            {
                String entry = file.next();
                
                ArrayList<String> row = new ArrayList<>(Arrays.asList(entry.split("\t")));
                
                Integer tokens = Integer.valueOf(row.get(0));
                String identity = String.join("\t", row.subList(1, Math.min(identity_length, row.size())));
                
                HashMap<String, Integer> spellings = new HashMap<>();
                
                for(int i = identity_length; i+1 < row.size(); i += 2)
                    spellings.put(row.get(i), Integer.parseInt(row.get(i+1)));
                
                Entry newentry = new Entry((double)tokens, identity);
                newentry.spellings = spellings; 
                
                list.add(newentry);
            }
            
            long total_tokens = 0;
            for(Entry entry : list)
                total_tokens += entry.count;
            for(Entry entry : list)
                entry.count *= 1000000/(double)total_tokens;
            
            collection.add(list);
        }
        
        update.accept("Aggregating collected lists...", -1.0);
        
        //double normalization_factor = 1000000/(double)total_tokens;
        //for(String key : aggregation.descendingKeySet())
        //    aggregation.replace(key, aggregation.get(key)*normalization_factor);
            
        
        ArrayList<Entry> mapping = merge(collection);
        
        update.accept("Sorting aggregated list...", -1.0);
        mapping.sort((a, b) -> (b.count - a.count > 0)?1:(b.count - a.count < 0)?-1:0);
        
        update.accept("Writing aggregated list to file...", -1.0);
        
        for(Entry entry : mapping)
        {
            if(entry.count != 0.0)
                println(out, String.format("%.10f", entry.count) + "\t" + entry.identity + "\t" + entry.spellingsAsString());
        }
        
        update.accept("Done", -1.0);
    }

    private static void println(BufferedWriter output, String text)
    {
        try
        {
            output.write(text+"\n");
        }
        catch (IOException e) { /* */ }
    }
}
