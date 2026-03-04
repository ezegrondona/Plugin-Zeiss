import ij.*;
import ij.gui.*;
import ij.gui.Roi.*;
import ij.gui.HistogramWindow.*;
import ij.measure.*;
import ij.process.*;
import ij.util.*;
import ij.util.Tools;
import ij.text.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.plugin.PlugIn;

import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.event.MouseListener;


import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.event.*;
import javax.swing.colorchooser.*;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/*  This class is used to detect the candidate gland structures in IHC image.
 *  The processing is working on the grey scale image without color.
 *  Thus, it can be worked on any stained IHC images.
 *
*/
public class nucleiGroup implements PlugIn {

	public ImagePlus imp1,  imp2,  imp3, imp4,imp5,imp6,imp7;
	public ImageProcessor ip1,ip2,ip3,ip4,ip5,ip6,ip7;
	int seed;//number of nuclei particles
	int mask;//number of lumen regions
	int count;//number of pixels grown in each iteration
	int start;//the started index of array which saving the growing particles
	float[] seedX=new float[10000];//start coordinates for nuclei particles
	float[] seedY=new float[10000];
	int[] seedCX;//the center coordinates of roughly separated nuclei particles
	int[] seedCY;
	int[][] SeedXY;//the whole image which includes nuclei regions
	int[][] lastgrowN,lastgrowL;//record the regino grow pixels in the iteration
	int[][] nucleilumen;//this arry record the label of seed which reached a lumen at this coordinate,this cooridinate belongs to lumen
	int[][] seedcrlt;//connected nuclei particles for each nuclei particle
	int[] indexofcrlt;//index of each nuclei particle

	float[] maskX=new float[10000];//start coordinates for lumen regions
	float[] maskY=new float[10000];
	int[] maskCX;//center cooridinates for lumen regions
    int[] maskCY;
	int[] masksize;//size of lumen region
	int[][] maskcrlt;//adjacent lumen regions for each lumen
	int[] indexofmaskcrlt;//index of lumen
	int[][] MaskXY;//the whole image which includes lumen regions

	public void exec(ImagePlus imp1, ImagePlus imp2){
		SeedXY=new int[imp1.getWidth()][imp1.getHeight()];
		MaskXY=new int[imp1.getWidth()][imp1.getHeight()];
		lastgrowN=new int[imp1.getWidth()][imp1.getHeight()];
		lastgrowL=new int[imp1.getWidth()][imp1.getHeight()];
		nucleilumen=new int[imp1.getWidth()][imp1.getHeight()];
		imp1.setTitle("Mask Image");

		ImageCalculator(imp1,imp2);
	//initial SeedXY and MaskXY
		for(int x=0;x<imp1.getWidth();x++)
			for(int y=0;y<imp1.getHeight();y++){
				SeedXY[x][y]=9999;
				MaskXY[x][y]=9999;
				lastgrowN[x][y]=9999;
				lastgrowL[x][y]=9999;
				nucleilumen[x][y]=9999;
			}
		//imp2.show();

	//Nuclei(seed) image processing
		Size(imp2,10,3000,0);//TMA30, WSL10
		ip2=imp2.getProcessor();
		ip2.invert();
		imp3=IJ.createImage("Seed", "8-bit", imp2.getWidth(), imp2.getHeight(), 1);
		ip3=imp3.getProcessor();
		count=0;
		seedCX=new int[seed];
		seedCY=new int[seed];
		for (int k = 0; k < seed; k++)
			Region(imp2,(int)seedX[k],(int)seedY[k],k,0);

	//Lumen(mask) image processing
		Size(imp1,10,50000,1);//TMA50,WSL10
		imp4=imp1.duplicate();
		imp4.setTitle("lumen");
		//imp4.show();
		imp5=IJ.createImage("smallbox", "8-bit", imp1.getWidth(), imp1.getHeight(), 1);
		imp6=IJ.createImage("largebox", "8-bit", imp1.getWidth(), imp1.getHeight(), 1);
		IJ.run(imp4,"Find Edges",null);
		ip4=imp4.getProcessor();
		ip4.invert();
		count=0;
		maskCX=new int[mask];
		maskCY=new int[mask];
		masksize=new int[mask];
		for (int k = 0; k < mask; k++)
			Region(imp4,(int)maskX[k],(int)maskY[k],k,1);

	//record nuclei xy coordinates

		seedcrlt=new int[seed][seed];
		indexofcrlt=new int[seed];
		maskcrlt=new int[mask][mask];
		indexofmaskcrlt=new int[mask];
		int lc=0;
		int stop=0;
		start=0;
		do{
			stop=0;
			stop+=GrowN(imp4,imp2,imp3,lc);
			lc++;
			IJ.showStatus("Iteration  "+lc);
		}while(stop!=0 && lc<5 );

		do{
			stop=0;
			stop+=GrowL(imp3,imp1,imp4,lc);
			lc++;
			IJ.showStatus("Iteration  "+lc);
		}while(stop!=0 && lc<20);

//*************************************************************

//Draw lines to the connected lumen
//some of the lumen regions are falsely separated in the first process
//This process is to connect the falsely separated lumens and draw lines to connect them
		ip1=imp1.getProcessor();
		ip1.setColor(100);
		int crx,cry;
		for(int i=0;i<mask;i++)
		  if(indexofmaskcrlt[i]>0){
		    for(int j=0;j<indexofmaskcrlt[i];j++){
			if(j<indexofmaskcrlt[i]){
			  crx=(int)maskCX[maskcrlt[i][j]];
		        cry=(int)maskCY[maskcrlt[i][j]];
			  ip1.drawLine((int)maskCX[i],(int)maskCY[i],crx,cry);
			}
		    }
		  }

//*****************************************************************

//find nuclei correspond to each luminal space,calculate radius from these nuclei center to the edge of lumen they reached
		ip4.invert();
		IJ.run(imp4,"Fill Holes",null);
		ip4.invert();

		int[][] dist=new int[seed][mask];
		int[][] maskedgex=new int[mask][100000];
		int[][] maskedgey=new int[mask][100000];
		int[] maskedge=new int[mask];
		int nid,lid,distance;
		for(int i=0;i<seed;i++)
		  for(int j=0;j<mask;j++)
			dist[i][j]=9999;
		for(int x=0;x<imp2.getWidth();x++)
		  for(int y=0;y<imp2.getHeight();y++)
			if(nucleilumen[x][y]!=9999){
			  nid=SeedXY[x][y];
			  lid=nucleilumen[x][y];
			  if(nid != 9999 && lid != 9999){
			    maskedgex[lid][maskedge[lid]]=x;
			    maskedgey[lid][maskedge[lid]]=y;
			    maskedge[lid]++;
			    distance=(int)Math.sqrt((x-seedCX[nid])*(x-seedCX[nid])+(y-seedCY[nid])*(y-seedCY[nid]));
			    if(distance<dist[nid][lid])
				 dist[nid][lid]=distance;
			  }
			}

//find the maximum distance from nuclei center to the luminal areas
		int[] maximum=new int[mask];
		for(int i=0;i	<mask;i++)
		  for(int j=0;j<seed;j++)
			if(dist[j][i]!=9999 && dist[j][i]>maximum[i])
			  maximum[i]=dist[j][i];
//dilate each luminal area with its maximum distance plus 5 pixels and find the covered nuclei center in this dilated area
		int[][] scenter=new int[imp1.getWidth()][imp1.getHeight()];
		int[][] maskseed=new int[mask][seed];
		int[] msid=new int[mask];
		for(int i=0;i<seed;i++)
			scenter[seedCX[i]][seedCY[i]]=1;

		for (int k = 0; k < mask; k++){
		  imp7=IJ.createImage("maskgrow", "8-bit", imp2.getWidth(), imp2.getHeight(), 1);
		  ip7=imp7.getProcessor();
		  ip7.setColor(0);
		  for(int i=0;i<maskedge[k];i++){
			int x=maskedgex[k][i];
			int y=maskedgey[k][i];
			int r=maximum[k];
			ip7.fillOval(x-r-5,y-r-5,2*r+10,2*r+10);
		  }
		  for(int x=0;x<imp2.getWidth();x++)
		    for(int y=0;y<imp2.getHeight();y++)
			if(ip7.get(x,y)==0 && scenter[x][y]==1 && SeedXY[x][y]!=9999){
			  maskseed[k][msid[k]]=SeedXY[x][y];
			  msid[k]++;
			}

		  imp7.changes=false;
		  imp7.close();
		}

//find toppoits for bounding box of each nuclei
		int[] ntopleftx=new int[seed];
		int[] ntoplefty=new int[seed];
		int[] ndownrightx=new int[seed];
		int[] ndownrighty=new int[seed];
		for(int i=0;i<seed;i++){
		  ntopleftx[i]=imp2.getWidth();
		  ntoplefty[i]=imp2.getHeight();
		}
		int mid,sid,tlx,tly,drx,dry;

		for(int x=0;x<imp2.getWidth();x++)
		  for(int y=0;y<imp2.getHeight();y++)
		    if(SeedXY[x][y]!=9999){
		      sid=SeedXY[x][y];
		      if(x<ntopleftx[sid])
			ntopleftx[sid]=x;
		      if(y<ntoplefty[sid])
			ntoplefty[sid]=y;
		      if(x>ndownrightx[sid])
			ndownrightx[sid]=x;
		      if(y>ndownrighty[sid])
			ndownrighty[sid]=y;
		    }
//find bounding box for each luminal area

		int[] gtopleftx=new int[mask];
		int[] gtoplefty=new int[mask];
		int[] gdownrightx=new int[mask];
		int[] gdownrighty=new int[mask];
		for(int i=0;i<mask;i++){
		  gtopleftx[i]=imp2.getWidth();
		  gtoplefty[i]=imp2.getHeight();
		}

			for(int i=0;i<mask;i++)
		  for(int j=0;j<msid[i];j++){
		      	mid=i;
			sid=maskseed[i][j];
		      if(seedCX[sid]<gtopleftx[mid])
			gtopleftx[mid]=seedCX[sid];
		      if(seedCY[sid]<gtoplefty[mid])
			gtoplefty[mid]=seedCY[sid];
		      if(seedCX[sid]>gdownrightx[mid])
			gdownrightx[mid]=seedCX[sid];
		      if(seedCY[sid]>gdownrighty[mid])
			gdownrighty[mid]=seedCY[sid];

		    }

//----------------------------

//re-evaluate the correlated lumen and find their bounding box coordiantes
int[] correlationmask=new int[mask];
int start,end;
IJ.showStatus("correlation calculation");
		for(int k=0;k<mask;k++){
		  if(indexofmaskcrlt[k]!=0){
			correlationmask=new int[mask];
			start=0;
			end=0;
			for(int j=0;j<indexofmaskcrlt[k];j++){
			  correlationmask[end]=maskcrlt[k][j];
			  end++;
			}

			for(int i=0;i<end;i++){
			  mid=correlationmask[i];
			  if(gtopleftx[mid]<gtopleftx[k])
			    gtopleftx[k]=gtopleftx[mid];
		        if(gtoplefty[mid]<gtoplefty[k])
			    gtoplefty[k]=gtoplefty[mid];
		        if(gdownrightx[mid]>gdownrightx[k])
			    gdownrightx[k]=gdownrightx[mid];
		        if(gdownrighty[mid]>gdownrighty[k])
			    gdownrighty[k]=gdownrighty[mid];

			}

		  }
		}
//----------------------------

		int[][] Rectmask=new int[mask][4];
		for(int i=0;i<mask;i++)
		  if(gtopleftx[i]!=imp1.getWidth() && gtoplefty[i]!=imp1.getHeight() && gdownrightx[i]!=0 && gdownrighty[i]!=0){
		    Rectmask[i][0]=gtopleftx[i];
		    Rectmask[i][1]=gtoplefty[i];
		    Rectmask[i][2]=gdownrightx[i];
		    Rectmask[i][3]=gdownrighty[i];
		}
		ip5=imp5.getProcessor();
		ip6=imp6.getProcessor();
		ip6.setColor(100);
		ip5.setColor(100);
		for(int i=0;i<mask;i++){
			ip5.drawRect(Rectmask[i][0],Rectmask[i][1],Rectmask[i][2]-Rectmask[i][0],Rectmask[i][3]-Rectmask[i][1]);
			}

		copyModel(Rectmask);
		IJ.showProgress(1.0);
		imp5.show();//result of small boxes
		imp1.changes=false;
		imp2.changes=false;
		imp3.changes=false;
		imp4.changes=false;
		imp5.changes=false;
		imp6.changes=false;
		imp7.changes=false;
		imp1.close();
		imp2.close();
		imp3.close();
		imp4.close();
		imp6.close();
		imp7.close();

	}


	public void run(String arg) {

	}

public void ImageCalculator(ImagePlus imp,ImagePlus imp2){
		ImageCalculator imagec=new ImageCalculator();
		imagec.run("Subtract",imp,imp2);

	}


//Remove noise nuclei particles or noise lumen regions
//Record start points for each nuclei particle and lumen region
//Record the number of nuclei particle and lumen region
	public void Size(ImagePlus imp, int size, int maxs,int key){
		Analyzer ana = new Analyzer();
        	ResultsTable rt = ana.getResultsTable();
        	rt.reset();
		ParticleAnalyzer pa = new ParticleAnalyzer();
		if(key==0)//seed=0,mask=1
        		pa = new ParticleAnalyzer(ParticleAnalyzer.RECORD_STARTS+ParticleAnalyzer.IN_SITU_SHOW+ParticleAnalyzer.SHOW_MASKS+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, ParticleAnalyzer.AREA, rt, size, Double.POSITIVE_INFINITY, 0.00, 1.00);
		if(key==1)
			pa = new ParticleAnalyzer(ParticleAnalyzer.RECORD_STARTS+ParticleAnalyzer.IN_SITU_SHOW+ParticleAnalyzer.SHOW_MASKS, ParticleAnalyzer.AREA, rt, size,Double.POSITIVE_INFINITY, 0.00, 1.00);

//ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
        	pa.analyze(imp);
		int t=0;
		if(key==0){
			
			seed = Analyzer.getCounter();
            //start coordinates for nuclei particles
			seedX = rt.getColumn(38);//x=38,y=39
    		seedY = rt.getColumn(39);//
		}
		if(key==1){
			mask = Analyzer.getCounter();
            //start coordinates for lumen regions
			maskX = rt.getColumn(38);//x=36,y=37
    		maskY = rt.getColumn(39);//x=36,y=37
		}
		if (IJ.isResultsWindow()) {
            		IJ.selectWindow("Results");
            		IJ.run("Close");
        	}
	}

//Record pixels corresponding to each nuclei particle and lumen region
//Region growi from the start point with 8 neighbour pixels
void Region(ImagePlus impO, int x, int y, int index,int key){
//impO=original, impD=drawing image
	int[] eightx = {1, 0, -1, 0, 1, 1, -1, -1};
      int[] eighty = {0, 1, 0, -1, 1, -1, 1, -1};
	int[] whitex = new int[1000000];
      int[] whitey = new int[1000000];
	whitex[0] = x;
      whitey[0] = y;
	int centerx=x;
	int centery=y;
	int m = 0;
	int s = 1;
	int start=count;
	count++;
	ImageProcessor ipO=impO.getProcessor();
	//ImageProcessor ipD=impD.getProcessor();
	do {
            for (int j = 0; j < 8; j++) {
		if((whitex[m]+eightx[j])>0 && (whitex[m]+eightx[j])<impO.getWidth() && (whitey[m]+eighty[j])>0 && (whitey[m]+eighty[j])<impO.getHeight()){
                if (ipO.get(whitex[m] + eightx[j], whitey[m] + eighty[j]) == 0) {
//0 is object (nuclei)
                    boolean exist = false;
                    for (int k = 0; k < s; k++) {
                        if (whitex[k] == whitex[m] + eightx[j] && whitey[k] == whitey[m] + eighty[j]) {
                            exist = true;
                        }
                    }
                    if (!exist) {
                        whitex[s] = whitex[m] + eightx[j];
                        whitey[s] = whitey[m] + eighty[j];
				centerx+=whitex[m] + eightx[j];
				centery+=whitey[m] + eighty[j];
                        s++;
				count++;
                    }

                }else {
			if(key==0)
			  lastgrowN[whitex[m]][whitey[m]]=0;
			if(key==1)
			  lastgrowL[whitex[m]][whitey[m]]=0;
			}
              }
            }
            m++;
        } while (m != s);
	if(key==0){
		seedCX[index]=(int)centerx/m;
		seedCY[index]=(int)centery/m;
		for(int i=0;i<m;i++){
			SeedXY[whitex[i]][whitey[i]]=index;
		}
	}
	if(key==1){
		maskCX[index]=(int)centerx/m;
		maskCY[index]=(int)centery/m;
		masksize[index]=m;
		for(int i=0;i<m;i++){
			MaskXY[whitex[i]][whitey[i]]=index;
		}
	}
}

//Region grow the nuclei with limited iterations to close the nuclei regions
int GrowN(ImagePlus Mask, ImagePlus Seed,ImagePlus Gpixel,int lc){

	  int s=start;
        int[] eightx = {1, 0, -1, 0, 1, 1, -1, -1};
        int[] eighty = {0, 1, 0, -1, 1, -1, 1, -1};
	  int c=0;
	  int[] bx=new int[1000000];
	  int[] by=new int[1000000];
      ImageProcessor ipmask = Mask.getProcessor();//lumen image
	ImageProcessor ipseed=Seed.getProcessor();//nuclei particle image
	ImageProcessor ipg=Gpixel.getProcessor();//nuclei growing image

	int sum=0;
	for(int x=0;x<Seed.getWidth();x++)
	  for(int y=0;y<Seed.getHeight();y++)
	    if(lastgrowN[x][y]==0){//3.1
		lastgrowN[x][y]=9999;
            for (int j = 0; j < 8; j++) {//3.2
		  if((x+eightx[j])>0 && (x+eightx[j])<Seed.getWidth() && (y+eighty[j])>0 && (y+eighty[j])<Seed.getHeight()){//3.3
                if (ipmask.get(x + eightx[j], y + eighty[j])==255){//3.4
		      if(ipg.get(x + eightx[j], y + eighty[j])==255 ) {
			  ipg.set(x + eightx[j], y + eighty[j], 0);
			  SeedXY[x+eightx[j]][y+eighty[j]]=SeedXY[x][y];
			  bx[c]=x+eightx[j];
			  by[c]=y+eighty[j];
			  c++;
		        }
		      else if(lc<1){
		        int nl=SeedXY[x+eightx[j]][y+eighty[j]];//the label of nuclei from 1 to the total number of nuclei
			  int nl1=SeedXY[x][y];
			  if(nl!=nl1){
			    boolean exist=false;
			    if(indexofcrlt[nl1]==0){
				seedcrlt[nl1][0]=nl;
				indexofcrlt[nl1]++;
			    }else{
			      for(int tmp1=0;tmp1<indexofcrlt[nl1];tmp1++)
			        if(seedcrlt[nl1][tmp1]==nl)
			          exist=true;
				  if(!exist){
				    seedcrlt[nl1][indexofcrlt[nl1]]=nl;
				    indexofcrlt[nl1]++;
				}
			    }
			  }
		      }
		    }//3.4
		    else{
		    }
		}//3.3
          }//3.2

	  }//3.1
	for(int i=0;i<c;i++)
		lastgrowN[bx[i]][by[i]]=0;


	if(c>0){
	  return 1;
	}else{
	  return 0;
	}
}

//Region grow of lumen regions
//Record the touched nuclei indexes
//Record the touched lumen indexes
int GrowL(ImagePlus Mask, ImagePlus Seed,ImagePlus Gpixel,int lc){

	  int s=start;
        int[] eightx = {1, 0, -1, 0, 1, 1, -1, -1};
        int[] eighty = {0, 1, 0, -1, 1, -1, 1, -1};
	  int c=0;
	  int[] bx=new int[1000000];
	  int[] by=new int[1000000];
      ImageProcessor ipmask = Mask.getProcessor();//lumen image
	ImageProcessor ipseed=Seed.getProcessor();//nuclei particle image
	ImageProcessor ipg=Gpixel.getProcessor();//nuclei growing image

	int sum=0;
	for(int x=0;x<Seed.getWidth();x++)
	  for(int y=0;y<Seed.getHeight();y++)
	    if(lastgrowL[x][y]==0){//3.1
		lastgrowL[x][y]=9999;
            for (int j = 0; j < 8; j++) {//3.2
		  if((x+eightx[j])>0 && (x+eightx[j])<Seed.getWidth() && (y+eighty[j])>0 && (y+eighty[j])<Seed.getHeight()){//3.3
                if (ipmask.get(x + eightx[j], y + eighty[j])==255){//3.4
		      if(ipg.get(x + eightx[j], y + eighty[j])==255 ) {
			  ipg.set(x + eightx[j], y + eighty[j], 0);
			  MaskXY[x+eightx[j]][y+eighty[j]]=MaskXY[x][y];
			  bx[c]=x+eightx[j];
			  by[c]=y+eighty[j];
			  c++;
		        } else if(lc<20 && MaskXY[x+eightx[j]][y+eighty[j]]!=9999){
		        int nl=MaskXY[x+eightx[j]][y+eighty[j]];//the label of nuclei from 1 to the total number of nuclei
			  int nl1=MaskXY[x][y];
			  if(nl!=nl1){
			    boolean exist=false;
			    if(indexofmaskcrlt[nl1]==0){
				maskcrlt[nl1][0]=nl;
				indexofmaskcrlt[nl1]++;
			    }else{
			      for(int tmp1=0;tmp1<indexofmaskcrlt[nl1];tmp1++)
			        if(maskcrlt[nl1][tmp1]==nl)
			          exist=true;
				  if(!exist){
				    maskcrlt[nl1][indexofmaskcrlt[nl1]]=nl;
				    indexofmaskcrlt[nl1]++;
				}
			    }
			  }
		      }


		     }//3.4
		  else
			nucleilumen[x+eightx[j]][y+eighty[j]]=MaskXY[x][y];//each reached pixel are signed by the labe of nuclei
		}//3.3
          }//3.2

	  }//3.1
	for(int i=0;i<c;i++)
		lastgrowL[bx[i]][by[i]]=0;


	if(c>0){
	  return 1;
	}else{
	  return 0;
	}
}

/**
  ***************************************************************
  *	save the results in txt file in output folder*
  ***************************************************************
*/
void copyModel(int[][] a) {
//void copyModel(int[] a){
                IJ.showStatus("Saving CPA values...");
	  Frame frame = new Frame();
        FileDialog fdhist = new FileDialog(frame, "save bounding box coordinates", FileDialog.SAVE);
        fdhist.setVisible(true);
        String path = fdhist.getDirectory();
        String filename = fdhist.getFile();
	  File filemodel = new File(path + filename);

        CharArrayWriter aw = new CharArrayWriter(256 * 4);
        try {
            if (!filemodel.exists()) {
                filemodel.createNewFile();

            }

            FileWriter writer = new FileWriter(filemodel);

            PrintWriter pw = new PrintWriter(writer);
		int c;
		for(int i=0;i<mask;i++){
			for(int j=0;j<4;j++){
				pw.print(a[i][j]+"\t");
			}
			pw.print("\n");
		}
                        String text = aw.toString();
            pw.close();
            StringSelection contents = new StringSelection(text);
            IJ.showStatus(text.length() + " characters saved to File");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

/*****************************************************************
 ****************************************************************
*/



}