// A Java program for Prim's Minimum Spanning Tree (MST) algorithm.
// The program is for adjacency matrix representation of the graph

package sail.g5;

import java.util.*;
import java.lang.*;
import java.io.*;
import sail.sim.Point;

public class Tree
{
	// Root of Tree
		Node root;

		public Tree()
		{
			root = null;
		}

		/* Given a tree, print its nodes in preorder*/
		public void preorder(Node node, ArrayList<Integer> order)
		{
			if (node == null)
				return;
			
			order.add(node.key);

			/* first print data of node */
			if(node.children != null){
				for(Node child: node.children){
					/* then recur on child */
					preorder(child, order);
				}
			}
		}

		public void preorder(ArrayList<Integer> order) {
			preorder(root, order);
		}

}