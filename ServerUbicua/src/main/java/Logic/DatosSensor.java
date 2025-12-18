package logic;

public class DatosSensor {
    private String currentState;
    private boolean pedestrianWaiting;
    private String time;

    public String getCurrentState() {return currentState;}

    public void setCurrentState(String currentState) {this.currentState = currentState;}

    public boolean isPedestrianWaiting() {return pedestrianWaiting;}

    public void setPedestrianWaiting(boolean pedestrianWaiting) {this.pedestrianWaiting = pedestrianWaiting;}
    
    public String getTime() {return time;}

    public void setTime(String time) {this.time = time;}
}