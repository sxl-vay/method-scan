package top.boking.methodscan.swing;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CustomFileChooser {

    public static void main(String[] args) {
        // 创建文件选择框
        JFileChooser fileChooser = new JFileChooser();
        
        // 创建自定义提示标签
        JLabel label = new JLabel("请选择一个文本文件（.txt）");

        // 将标签放到对话框中
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.NORTH);
        panel.add(fileChooser, BorderLayout.CENTER);

        // 创建自定义的对话框
        JOptionPane.showConfirmDialog(null, panel, "文件选择", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
    }
}
