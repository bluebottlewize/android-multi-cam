package com.bluebottle.multicam;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoItem
{
    public String name;
    public String path_one, path_two;
    public String lid, pid1, pid2;

    public VideoItem(String name, String path_one, String path_two)
    {
        this.name = name;
        this.path_one = path_one;
        this.path_two = path_two;

        lid = "LID :\t\t";
        pid1 = "PID :\t\t";
        pid2 = "PID :\t\t";

        String regex = "^(VID_\\d{8}_\\d{6})_(\\d*)_(\\d*)\\.mp4$";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(path_one);
        if (matcher.matches())
        {
            lid += matcher.group(2);
            pid1 += matcher.group(3);
        }

        matcher = pattern.matcher(path_two);
        if (matcher.matches())
        {
            pid2 += matcher.group(3);
        }
    }
}