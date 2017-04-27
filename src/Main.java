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

public class Main
{

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
            
            double normalization_factor = 1000000/(double)total_tokens;
            
            for(Entry entry : list)
                entry.count = entry.count*normalization_factor;
            
            collection.add(list);
        }
        
        update.accept("Aggregating collected lists...", -1.0);
        
        TreeMap<String, Double> aggregation = new TreeMap<>();
        
        Double total_tokens = 0.0;
        
        for(ArrayList<Entry> list : collection)
        {
            for(Entry entry : list)
            {
                if(aggregation.containsKey(entry.identity))
                    aggregation.replace(entry.identity, aggregation.get(entry.identity)+entry.count);
                else
                    aggregation.put(entry.identity, entry.count);
                total_tokens += entry.count;
            }
        }
            
        double normalization_factor = 1000000/(double)total_tokens;
        
        for(String key : aggregation.descendingKeySet())
            aggregation.replace(key, aggregation.get(key)*normalization_factor);
        
        update.accept("Sorting aggregated list...", -1.0);
        
        ArrayList<Entry> mapping = new ArrayList<>();
        
        for(Map.Entry<String, Double> entry : aggregation.entrySet())
            mapping.add(new Entry(entry.getValue(), entry.getKey()));
        mapping.sort((a, b) -> (b.count - a.count > 0)?1:(b.count - a.count < 0)?-1:0);
        
        update.accept("Writing aggregated list to file...", -1.0);
        
        for(Entry entry : mapping)
            println(out, entry.count.toString() + "\t" + entry.identity);
        
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
