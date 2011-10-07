package org.noisemap.core;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 06_10_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 * @author Chris Rathman
 * @see http://www.angelfire.com/tx4/cus/regress/
 ***********************************/

public class Regression {
  
   // Apply least squares to raw data to determine the coefficients for
   // an n-order equation: y = a0*X^0 + a1*X^1 + ... + an*X^n.
   // Returns the coefficients for the solved equation, given a number
   // of y and x data points. The rawData input is given in the form of
   // {{y0, x0}, {y1, x1},...,{yn, xn}}.   The coefficients returned by
   // the regression are {a0, a1,...,an} which corresponds to
   // {X^0, X^1,...,X^n}. The number of coefficients returned is the
   // requested equation order (norder) plus 1.
   static double[] linear_equation(double rawData[][], int norder) {
      double a[][] = new double[norder+1][norder+1];
      double b[] = new double[norder+1];
      double term[] = new double[norder+1];
      double ysquare = 0;

      // step through each raw data entries
      for (int i = 0; i < rawData.length; i++) {

         // sum the y values
         b[0] += rawData[i][0];
         ysquare += rawData[i][0] * rawData[i][0];

         // sum the x power values
         double xpower = 1;
         for (int j = 0; j < norder+1; j++) {
            term[j] = xpower;
            a[0][j] += xpower;
            xpower *= rawData[i][1];
         }

         // now set up the rest of rows in the matrix - multiplying each row by each term
         for (int j = 1; j < norder+1; j++) {
            b[j] += rawData[i][0] * term[j];
            for (int k = 0; k < b.length; k++) {
               a[j][k] += term[j] * term[k];
            }
         }
      }

      // solve for the coefficients
      double coef[] = gauss(a, b);

      // calculate the r-squared statistic
      double ss = 0;
      double yaverage = b[0] / rawData.length;
      for (int i = 0; i < norder+1; i++) {
         double xaverage = a[0][i] / rawData.length;
         ss += coef[i] * (b[i] - (rawData.length * xaverage * yaverage));
      }
      
      // solve the simultaneous equations via gauss
      return coef;
   }

   // it's been so long since I wrote this, that I don't recall the math
   // logic behind it. IIRC, it's just a standard gaussian technique for
   // solving simultaneous equations of the form: |A| = |B| * |C| where we
   // know the values of |A| and |B|, and we are solving for the coefficients
   // in |C|
   static double[] gauss(double ax[][], double bx[]) {
      double a[][] = new double[ax.length][ax[0].length];
      double b[] = new double[bx.length];
      double pivot;
      double mult;
      double top;
      int n = b.length;
      double coef[] = new double[n];

      // copy over the array values - inplace solution changes values
      for (int i = 0; i < ax.length; i++) {
         System.arraycopy(ax[i], 0, a[i], 0, ax[i].length);
         b[i] = bx[i];
      }

      for (int j = 0; j < (n-1); j++) {
         pivot = a[j][j];
         for (int i = j+1; i < n; i++) {
            mult = a[i][j] / pivot;
            for (int k = j+1; k < n; k++) {
                a[i][k] -= mult * a[j][k];
            }
            b[i] -= mult * b[j];
         }
      }

      coef[n-1] = b[n-1] / a[n-1][n-1];
      for (int i = n-2; i >= 0; i--) {
         top = b[i];
         for (int k = i+1; k < n; k ++) {
            top -= a[i][k] * coef[k];
         }
         coef[i] = top / a[i][i];
      }
      return coef;
   }

}