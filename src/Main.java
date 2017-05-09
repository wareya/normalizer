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

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.StrictMath.sqrt;

public class Main
{
    static boolean alternate_normalization = false;
    static private class Entry
    {
        Double count;
        String identity;
        Entry(Double count, String identity)
        {
            this.count = count;
            this.identity = identity;
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
                update.accept("Failed to open an input file as UTf-8.", 0.0);
                return;
            }
            Integer total_tokens = 0; 
            
            for(String entry; file.hasNext();)
            {
                entry = file.next();
                //System.out.println(entry);
                //System.out.println(entry.split("\t", 2).length);
                //if(true) return;
                
                Integer tokens = Integer.valueOf(entry.split("\t", 2)[0]);
                String identity = entry.split("\t", 2)[1];
                
                total_tokens += tokens;
                
                list.add(new Entry((double)tokens, identity));
            }
            
            for(Entry entry : list)
                entry.count *= 1000000/(double)total_tokens;
            
            collection.add(list);
        }
        
        update.accept("Aggregating collected lists...", -1.0);
        
        TreeMap<String, Double> aggregation = new TreeMap<>();
        
        if(!alternate_normalization)
        {
            Double total_tokens = 0.0;
            for(ArrayList<Entry> list : collection)
            {
                for(Entry entry : list)
                {
                    total_tokens += entry.count;
                    //if(punish_rare_words) entry.count = sqrt(entry.count); // this reduces the effect large values in a single book has on the frequency listing
                    if(aggregation.containsKey(entry.identity))
                        aggregation.replace(entry.identity, aggregation.get(entry.identity)+entry.count);
                    else
                        aggregation.put(entry.identity, entry.count);
                }
            }
            for(String key : aggregation.descendingKeySet())
                aggregation.replace(key, aggregation.get(key)*1000000/total_tokens);
        }
        else
        {
            HashMap<String, ArrayList<Double>> terms = new HashMap<>();
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
                Double tokens;
                if(counts.size()%2 == 1)
                    tokens = counts.get(counts.size()/2);
                else
                    tokens = (counts.get(counts.size()/2)+counts.get(counts.size()/2-1))/2;
                aggregation.put(entry.getKey(), tokens);
                total_tokens += tokens;
            }
            for(String key : aggregation.descendingKeySet())
                aggregation.replace(key, aggregation.get(key)*1000000/total_tokens);
        }
        
        //double normalization_factor = 1000000/(double)total_tokens;
        //for(String key : aggregation.descendingKeySet())
        //    aggregation.replace(key, aggregation.get(key)*normalization_factor);
            
        update.accept("Sorting aggregated list...", -1.0);
        
        ArrayList<Entry> mapping = new ArrayList<>();
        
        for(Map.Entry<String, Double> entry : aggregation.entrySet())
            mapping.add(new Entry(entry.getValue(), entry.getKey()));
        mapping.sort((a, b) -> (b.count - a.count > 0)?1:(b.count - a.count < 0)?-1:0);
        
        update.accept("Writing aggregated list to file...", -1.0);
        
        for(Entry entry : mapping)
        {
            if(entry.count != 0.0)
                println(out, entry.count.toString() + "\t" + entry.identity);
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
