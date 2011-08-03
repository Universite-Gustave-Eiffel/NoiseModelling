/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.noisemap.run;

import org.apache.log4j.Logger;

/**
 *
 * @author fortin
 */
public class ConsoleLogger extends Logger {

    public ConsoleLogger(String name) {
        super(name);
    }


  @Override
    public
  void info(Object message) {
    System.out.println(message);
  }

  @Override
    public
  void warn(Object message) {
    System.err.println(message);
  }

  @Override
    public
  void error(Object message) {
    System.err.println(message);
  }


}
