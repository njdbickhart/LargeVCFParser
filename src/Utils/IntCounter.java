/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

/**
 *
 * @author desktop
 */
public class IntCounter {
    private int count = 0;
    
    public void increment(){
        this.count++;
    }
    
    public int getCount(){
        return this.count;
    }
    
    public void setCount(int c){
        this.count = c;
    }
}
