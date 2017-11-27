// A Java program for Prim's Minimum Spanning Tree (MST) algorithm.
// The program is for adjacency matrix representation of the graph

package sail.g5;

import java.util.*;
import java.lang.*;
import java.io.*;
import sail.sim.Point;

public class Node
{
	int key;
	ArrayList<Node> children;

	public Node(int item)
	{
		key = item;
		children = null;
	}
}