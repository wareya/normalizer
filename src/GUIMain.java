import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.function.BiFunction;

import static java.lang.Math.min;

/*
 * Licensed under a public domain‐like license. See Main.java for license text.
 */

public class GUIMain extends Main {
    private static Thread worker = null;

    private static File[] files;
    static private JCheckBox option_simple_average;
    static private JCheckBox option_cropped_average;
    static private JCheckBox option_use_median;
    static private JCheckBox option_use_proportional_mean;
    static private JTextField option_custom_exponent;

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater( () ->
        {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException|InstantiationException|IllegalAccessException|UnsupportedLookAndFeelException e) { /* */ }

            Image icon = new ImageIcon(Main.class.getResource("icon.png")).getImage();
            JFrame window = new JFrame();
            window.setIconImage(icon);
            window.setTitle("frequency list normalizer/merger");
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            window.setResizable(false);

            Container pane = window.getContentPane();
            pane.setLayout(null);
            pane.setSize(400, 300);

            JLabel explanation1 = new JLabel("frequency list normalizer/merger");
            JLabel explanation2 = new JLabel("Input must be in UTF-8.");

            JButton input = new JButton("New input");
            JTextField field_input = new JTextField("");
            JButton doinput = new JButton("+");

            JButton write = new JButton("Output");
            JTextField field_write = new JTextField("");

            DefaultTableModel list = new DefaultTableModel();
            list.addColumn("Filename");

            JTable table  = new JTable(list) {
                public void editingStopped(ChangeEvent e)
                {
                    super.editingStopped(e);
                    for(int i = 0; i < list.getRowCount(); i++)
                    {
                        if(list.getValueAt(i, 0).equals(""))
                        {
                            list.removeRow(i);
                            i--;
                        }
                    }
                }
            };
            
            option_simple_average = new JCheckBox("Simple average (pretend these are radio buttons!)", false);
            option_cropped_average = new JCheckBox("Remove outliers (wants 5+ input lists or it's just median)", true);
            option_use_median = new JCheckBox("Use median instead of average (use carefully)", false);
            option_use_proportional_mean = new JCheckBox("Skew distributions by exponent before averaging:", false);
            option_custom_exponent = new JTextField("0.2");

            JScrollPane listPane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            JButton run = new JButton("Run");
            JProgressBar progress = new JProgressBar();

            input.setMargin(new Insets(5,5,5,5));
            write.setMargin(new Insets(5,5,5,5));
            doinput.setMargin(new Insets(0,0,0,0));
            progress.setStringPainted(true);
            progress.setString("Waiting to be run");

            input.addActionListener((e)->
            {
                FileDialog d = new FileDialog((java.awt.Frame) null, "Corpus (input)", FileDialog.LOAD);
                d.setMultipleMode(true);
                d.setVisible(true);
                if(d.getFiles() != null)
                    files = d.getFiles();
                if(files.length > 1)
                {
                    field_input.setEditable(false);
                    field_input.setText("<list of files>");
                }
                else if(files.length == 1)
                {
                    field_input.setEditable(true);
                    field_input.setText(files[0].getAbsolutePath());
                }
            });
            write.addActionListener((e)->
            {
                FileDialog d = new java.awt.FileDialog((java.awt.Frame) null, "Frequency list (output)", FileDialog.SAVE);
                d.setVisible(true);
                if(d.getFile() != null)
                    field_write.setText(d.getDirectory()+d.getFile());
            });
            doinput.addActionListener((e)->
            {
                if(files != null && files.length > 1)
                {
                    for(File f : files)
                        if(!f.getAbsolutePath().equals(""))
                            list.addRow(new String[]{f.getAbsolutePath()});
                }
                else if(!field_input.getText().equals(""))
                    list.addRow(new String[]{field_input.getText()});
                files = null;
                field_input.setEditable(true);
                field_input.setText("");
            });


            run.addActionListener((a)->
            {
                simple_average = option_simple_average.isSelected();
                cropped_average = option_cropped_average.isSelected();
                median = option_use_median.isSelected();
                proportional_mean = option_use_proportional_mean.isSelected();
                
                if(option_custom_exponent.getText() != null && !option_custom_exponent.getText().equals(""))
                    custom_exponent = Double.valueOf(option_custom_exponent.getText());
                
                if(worker != null && worker.isAlive()) return;
                worker = new Thread(() ->
                {
                    try
                    {
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(field_write.getText()), "UTF-8"));
                        ArrayList<String> inputs = new ArrayList<>();
                        for (int count = 0; count < list.getRowCount(); count++)
                        {
                            inputs.add(list.getValueAt(count, 0).toString());
                            //System.out.println(list.getValueAt(count, 0).toString());
                        }
                        if(inputs.size() > 0)
                        {
                            run(inputs, writer, (text, length) ->
                            {
                                progress.setString(text);
                                if(text.equals("Done"))
                                {
                                    progress.setIndeterminate(false);
                                    progress.setValue(0);
                                }
                                else if(length >= 0.0)
                                {
                                    progress.setIndeterminate(false);
                                    progress.setMaximum(100000000);
                                    progress.setValue((int)(length*100000000));
                                }
                                else
                                {
                                    progress.setIndeterminate(true);
                                }
                            });
                        }
                        writer.close();
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        progress.setString("Failed to open output as UTF-8.");
                    }
                    catch (FileNotFoundException e)
                    {
                        progress.setString("Failed to open output file.");
                    }
                    catch (IOException e)
                    {
                        progress.setString("Error while closing output file.");
                    }
                });
                worker.start();
            });

            // adds fullwidth elements
            BiFunction<JComponent, Integer, Integer> adder = (element, y) ->
            {
                element.setBounds(5, y, min(pane.getWidth()-10, element.getPreferredSize().width), element.getPreferredSize().height);
                return y + element.getPreferredSize().height;
            };
            Integer row = 5;

            explanation1.setBounds(5, 5, pane.getWidth()-10, 20); explanation1.setHorizontalAlignment(SwingConstants.CENTER); row += 25;

            row = adder.apply(explanation2, row); row += 5;
            input.setBounds(5, row, 65, 20); field_input.setBounds(75, row, pane.getWidth()-75-10-20-5, 20); doinput.setBounds(pane.getWidth()-20-10, row, 20, 20); row += 25;
            write.setBounds(5, row, 65, 20); field_write.setBounds(75, row, pane.getWidth()-75-10, 20); row += 25;

            row += 5;

            row = adder.apply(option_simple_average, row);
            row = adder.apply(option_cropped_average, row);
            row = adder.apply(option_use_median, row);
            option_custom_exponent.setBounds(option_use_proportional_mean.getPreferredSize().width+10, row, 40, 20);
            row = adder.apply(option_use_proportional_mean, row);

            listPane.setBounds(5, row, pane.getWidth()-10, 140);
            row += 150;

            run.setBounds(5, row, 65, 20); progress.setBounds(75, row, pane.getWidth()-75-10, 20); row += 25;

            pane.add(explanation1);
            pane.add(explanation2);

            pane.add(input);
            pane.add(field_input);
            pane.add(doinput);
            
            pane.add(write);
            pane.add(field_write);

            pane.add(option_simple_average);
            pane.add(option_cropped_average);
            pane.add(option_use_median);
            pane.add(option_custom_exponent);
            pane.add(option_use_proportional_mean);
            
            pane.add(listPane);

            pane.add(run);
            pane.add(progress);


            pane.setPreferredSize(new Dimension(400, row));
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
