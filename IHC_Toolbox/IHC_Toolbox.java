import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class IHC_Toolbox implements PlugIn {
	static boolean showArgs = true;

	public void run(String arg) {
            
		String msg = "";
		if (arg.equals("plugins"))
			msg = "Plugins>IHC Toolbox (Plugins)";
       	Immunostaining_toolbox it=new Immunostaining_toolbox(); 

	}
	

}
