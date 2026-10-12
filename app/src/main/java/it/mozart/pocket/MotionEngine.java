package it.mozart.pocket;

import java.time.LocalDate;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Deterministic idle → walk → idle → action cycle, with persistent facing and musical effects. */
public final class MotionEngine {
    public enum Pose {
        IDLE(0,4,2,"Riposo",false), WALK(1,8,8,"Passeggiata",false),
        SCORE(2,4,3,"Spartito",true), PIANO(3,4,5,"Pianoforte",true),
        BOW(4,4,2.5f,"Inchino",false), BIRTHDAY(5,4,4,"Compleanno",true),
        VIOLIN(6,4,5,"Violino",true), CONDUCT(7,4,4,"Direttore",true),
        FLUTE(8,4,4,"Flauto",true), QUILL(9,4,3.5f,"Composizione",true), HUM(10,4,3,"Canticchia",true),
        TEA(11,4,2.5f,"Un tè",false), BOOK(12,4,2,"Lettura",false), LETTER(13,4,2.5f,"Lettera",false),
        WATCH(14,4,2.5f,"Orologio",false), WIG(15,4,3,"Parrucca",false),
        DANCE(16,4,5,"Minuetto",true), STRETCH(17,4,2.5f,"Si stiracchia",false),
        YAWN(18,4,2,"Sbadiglio",false), NAP(19,4,1.5f,"Sonnellino",false), WAVE(20,4,4,"Saluto",false),
        TRICORN(21,4,3,"Tricorno",false), ROSE(22,4,2.5f,"Una rosa",false),
        MUSIC_BOX(23,4,4,"Carillon",true), TRAVEL(24,4,3,"In viaggio",false), APPLAUSE(25,4,5,"Applauso",true);
        public final int row, frameCount;
        public final float framesPerSecond;
        public final String label;
        public final boolean musical;
        Pose(int row, int frames, float fps, String label, boolean musical) {
            this.row=row; frameCount=frames; framesPerSecond=fps; this.label=label; this.musical=musical;
        }
    }
    public enum Occasion { NONE, BIRTHDAY, MAGIC_FLUTE, DON_GIOVANNI, REMEMBRANCE }
    private enum Cycle { IDLE_BEFORE_WALK, WALKING, IDLE_BEFORE_ACTION, ACTION }
    static final float IDLE_BRIDGE_SECONDS = .65f, NOTE_SECONDS = 2.4f;
    private static final long RANDOM_MASK = (1L << 48) - 1;
    private long randomState, effectState;
    private float width=1, height=1, size=1, x, y, targetX, targetY;
    private float time, remaining=3, speed=30, birthdayTimer, walkPhase;
    private float requestedSeconds=7, noteDelay, noteAge=-1, idlePreviewSeconds;
    private int noteVariant;
    private boolean initialized, facingLeft, specialDates=true, noteLeft, inPlaceWalk;
    private Pose pose=Pose.IDLE, requested;
    private Cycle cycle=Cycle.IDLE_BEFORE_WALK;
    private LocalDate date=LocalDate.now();

    public MotionEngine(long seed) {
        randomState=(seed ^ 0x5DEECE66DL) & RANDOM_MASK;
        effectState=(seed ^ 0xBADC0FFEE0DL) & RANDOM_MASK;
        noteDelay=5 + nextEffectRandom()*4;
    }
    private float nextRandom() {
        randomState=(randomState*0x5DEECE66DL+11)&RANDOM_MASK;
        return (randomState>>>24)*0x1.0p-24f;
    }
    private float nextEffectRandom() {
        effectState=(effectState*0x5DEECE66DL+11)&RANDOM_MASK;
        return (effectState>>>24)*0x1.0p-24f;
    }
    MotionEngine copy() {
        MotionEngine c=new MotionEngine(0);
        c.randomState=randomState; c.effectState=effectState;
        c.width=width; c.height=height; c.size=size; c.x=x; c.y=y; c.targetX=targetX; c.targetY=targetY;
        c.time=time; c.remaining=remaining; c.speed=speed; c.birthdayTimer=birthdayTimer; c.walkPhase=walkPhase;
        c.requestedSeconds=requestedSeconds; c.noteDelay=noteDelay; c.noteAge=noteAge; c.idlePreviewSeconds=idlePreviewSeconds;
        c.noteVariant=noteVariant;
        c.initialized=initialized; c.facingLeft=facingLeft; c.specialDates=specialDates; c.noteLeft=noteLeft; c.inPlaceWalk=inPlaceWalk;
        c.pose=pose; c.requested=requested; c.cycle=cycle; c.date=date;
        return c;
    }
    void writeState(DataOutput out) throws IOException {
        out.writeLong(randomState); out.writeLong(effectState);
        for (float v:new float[]{width,height,size,x,y,targetX,targetY,time,remaining,speed,birthdayTimer,
                walkPhase,requestedSeconds,noteDelay,noteAge,idlePreviewSeconds}) out.writeFloat(v);
        out.writeBoolean(initialized); out.writeBoolean(facingLeft); out.writeBoolean(specialDates);
        out.writeBoolean(noteLeft); out.writeBoolean(inPlaceWalk);
        out.writeInt(pose.ordinal()); out.writeInt(requested==null?-1:requested.ordinal()); out.writeInt(cycle.ordinal());
        out.writeInt(noteVariant); out.writeLong(date.toEpochDay());
    }
    static MotionEngine readState(DataInput in) throws IOException {
        MotionEngine e=new MotionEngine(0);
        e.randomState=in.readLong(); e.effectState=in.readLong();
        float[] v=new float[16];
        for (int i=0;i<v.length;i++) { v[i]=in.readFloat(); if(!Float.isFinite(v[i])) throw new IOException("Invalid motion state"); }
        if(e.randomState<0 || e.randomState>RANDOM_MASK || e.effectState<0 || e.effectState>RANDOM_MASK
                || v[0]<1 || v[1]<1 || v[2]<1 || v[9]<1 || v[12]<.2f || v[15]<0)
            throw new IOException("Invalid motion state");
        e.width=v[0]; e.height=v[1]; e.size=v[2]; e.x=v[3]; e.y=v[4]; e.targetX=v[5]; e.targetY=v[6];
        e.time=v[7]; e.remaining=v[8]; e.speed=v[9]; e.birthdayTimer=v[10]; e.walkPhase=v[11];
        e.requestedSeconds=v[12]; e.noteDelay=v[13]; e.noteAge=v[14]; e.idlePreviewSeconds=v[15];
        e.initialized=in.readBoolean(); e.facingLeft=in.readBoolean(); e.specialDates=in.readBoolean();
        e.noteLeft=in.readBoolean(); e.inPlaceWalk=in.readBoolean();
        int p=in.readInt(), r=in.readInt(), phase=in.readInt(); e.noteVariant=in.readInt();
        if(p<0 || p>=Pose.values().length || r!=-1 && (r<2 || r>=Pose.values().length)
                || phase<0 || phase>=Cycle.values().length || e.noteVariant<0 || e.noteVariant>2)
            throw new IOException("Invalid animation state");
        e.pose=Pose.values()[p]; e.requested=r==-1?null:Pose.values()[r]; e.cycle=Cycle.values()[phase];
        if(e.cycle==Cycle.WALKING && e.pose!=Pose.WALK || e.cycle==Cycle.ACTION && e.pose.row<2
                || (e.cycle==Cycle.IDLE_BEFORE_WALK || e.cycle==Cycle.IDLE_BEFORE_ACTION) && e.pose!=Pose.IDLE)
            throw new IOException("Invalid cycle state");
        try { e.date=LocalDate.ofEpochDay(in.readLong()); }
        catch(java.time.DateTimeException ex) { throw new IOException("Invalid date",ex); }
        return e;
    }
    public void setBounds(float w,float h,float desiredSize) {
        width=finitePositive(w); height=finitePositive(h);
        size=Math.min(finitePositive(desiredSize),Math.min(width,height));
        if(!initialized) { x=(width-size)/2; y=(height-size)/2; initialized=true; }
        x=clamp(x,0,maxX()); y=clamp(y,0,maxY()); targetX=clamp(targetX,0,maxX()); targetY=clamp(targetY,0,maxY());
    }
    private static float finitePositive(float v) { return Float.isFinite(v)?Math.max(1,v):1; }
    private static float clamp(float v,float lo,float hi) { return Math.max(lo,Math.min(v,hi)); }
    private float maxX() { return Math.max(0,width-size); }
    private float maxY() { return Math.max(0,height-size); }
    public void setSpeed(float v) { speed=finitePositive(v); }
    public void setDate(LocalDate v) { if(v!=null) date=v; }
    public void setSpecialDates(boolean v) { specialDates=v; }
    public static Occasion occasion(LocalDate d) {
        if(d.getMonthValue()==1 && d.getDayOfMonth()==27) return Occasion.BIRTHDAY;
        if(d.getMonthValue()==9 && d.getDayOfMonth()==30) return Occasion.MAGIC_FLUTE;
        if(d.getMonthValue()==10 && d.getDayOfMonth()==29) return Occasion.DON_GIOVANNI;
        if(d.getMonthValue()==12 && d.getDayOfMonth()==5) return Occasion.REMEMBRANCE;
        return Occasion.NONE;
    }
    public void update(float deltaSeconds) {
        if(!Float.isFinite(deltaSeconds) || deltaSeconds<=0) return;
        float dt=Math.min(.12f,deltaSeconds);
        birthdayTimer-=dt; updateNotes(dt); time+=dt; remaining-=dt;
        if(cycle==Cycle.WALKING) {
            // If the sprite fills its entire area, complete a short walk in place.
            if(inPlaceWalk) walkPhase=(walkPhase+dt)%1;
            else {
                float dx=targetX-x,dy=targetY-y,distance=(float)Math.hypot(dx,dy);
                if(distance<=.0001f) {
                    if(time>=.35f) remaining=0;
                    else walkPhase=(walkPhase+dt)%1;
                }
                else {
                    float step=Math.min(distance,speed*dt);
                    x+=dx/distance*step; y+=dy/distance*step;
                    walkPhase=(walkPhase+step/Math.max(1,size*.30f))%1;
                    if(step>=distance) { x=targetX; y=targetY; if(time>=.35f) remaining=0; }
                }
            }
        }
        x=clamp(x,0,maxX()); y=clamp(y,0,maxY());
        if(remaining<=0) {
            switch(cycle) {
                case IDLE_BEFORE_WALK: startWalk(); break;
                case WALKING: idle(Cycle.IDLE_BEFORE_ACTION,1+nextRandom()*1.3f); break;
                case IDLE_BEFORE_ACTION: startAction(); break;
                case ACTION: idle(Cycle.IDLE_BEFORE_WALK,1.8f+nextRandom()*1.8f); break;
            }
        }
    }
    private void startWalk() {
        inPlaceWalk=maxX()+maxY()<.5f;
        if(inPlaceWalk) { targetX=x; targetY=y; force(Pose.WALK,1.2f); return; }
        float farthest=(float)Math.hypot(Math.max(x,maxX()-x),Math.max(y,maxY()-y));
        float minimum=Math.min(size*.25f,farthest*.5f);
        for(int attempt=0;attempt<12;attempt++) {
            targetX=nextRandom()*maxX(); targetY=nextRandom()*maxY();
            if(Math.hypot(targetX-x,targetY-y)>=minimum) break;
        }
        if(Math.hypot(targetX-x,targetY-y)<minimum) {
            targetX=x<maxX()/2?maxX():0; targetY=y<maxY()/2?maxY():0;
        }
        force(Pose.WALK,(float)Math.hypot(targetX-x,targetY-y)/speed+.5f);
    }
    private void startAction() {
        if(requested!=null) {
            Pose next=requested; float seconds=requestedSeconds; requested=null;
            force(next,seconds); return;
        }
        Occasion event=specialDates?occasion(date):Occasion.NONE;
        if(event==Occasion.BIRTHDAY && birthdayTimer<=0) { force(Pose.BIRTHDAY,7); birthdayTimer=38; return; }
        float choice=nextRandom();
        if(event==Occasion.REMEMBRANCE && choice<.75f) force(Pose.SCORE,6);
        else if((event==Occasion.MAGIC_FLUTE || event==Occasion.DON_GIOVANNI) && choice<.65f) force(Pose.PIANO,8);
        else {
            int index=Math.min(22,(int)(choice*23));
            Pose next=Pose.values()[index<3?index+2:index+3];
            force(next,4+nextRandom()*4);
        }
    }
    private void idle(Cycle next,float seconds) {
        force(Pose.IDLE,Math.max(seconds,idlePreviewSeconds));
        cycle=next; idlePreviewSeconds=0;
    }
    /** Preview choices wait for the next action slot and never bypass a walk or pause. */
    public void request(Pose next,float seconds) {
        if(next==null) return;
        seconds=Float.isFinite(seconds)?Math.max(.2f,seconds):7;
        if(next==Pose.IDLE) {
            if(pose==Pose.IDLE) remaining=Math.max(remaining,seconds);
            else idlePreviewSeconds=seconds;
            return;
        }
        if(next==Pose.WALK) {
            requested=null;
            if(cycle==Cycle.IDLE_BEFORE_WALK) remaining=Math.min(remaining,IDLE_BRIDGE_SECONDS);
            return;
        }
        requested=next; requestedSeconds=seconds;
        if(pose==Pose.IDLE) remaining=Math.min(remaining,IDLE_BRIDGE_SECONDS);
    }
    // Direct setup for restoration and motion diagnostics. Automatic playback follows the cycle.
    void force(Pose next,float seconds) {
        pose=next; time=0; walkPhase=0;
        remaining=Math.max(.2f,seconds);
        cycle=next==Pose.IDLE?Cycle.IDLE_BEFORE_WALK:next==Pose.WALK?Cycle.WALKING:Cycle.ACTION;
        if(next==Pose.WALK) {
            inPlaceWalk=maxX()+maxY()<.5f;
            if(!inPlaceWalk && Math.hypot(targetX-x,targetY-y)<.0001f) {
                targetX=x<maxX()/2?maxX():0; targetY=clamp(y+maxY()/4,0,maxY());
            }
            if(Math.abs(targetX-x)>.0001f) facingLeft=targetX<x;
        }
    }
    public int frame() {
        if(pose==Pose.WALK) return Math.min(7,(int)(walkPhase*8));
        if(pose==Pose.IDLE) {
            // One two-second breathing loop; only the rig's upper body changes between frames.
            return ((int)(time*pose.framesPerSecond))%pose.frameCount;
        }
        int tick=(int)(time*pose.framesPerSecond);
        if(pose.row>=6) { int p=tick%6; return p<4?p:6-p; }
        return tick%pose.frameCount;
    }
    private void updateNotes(float dt) {
        noteDelay-=dt;
        if(noteAge>=0) { noteAge+=dt; if(noteAge>=NOTE_SECONDS) noteAge=-1; }
        if(noteAge<0 && noteDelay<=0 && (pose.musical || pose==Pose.IDLE)) {
            noteAge=0; noteVariant=Math.min(2,(int)(nextEffectRandom()*3)); noteLeft=nextEffectRandom()<.5f;
            noteDelay=8+nextEffectRandom()*12;
        }
    }
    public int noteAlpha() {
        if(noteAge<0) return 0;
        float p=noteAge/NOTE_SECONDS;
        return Math.round(255*Math.min(1,Math.min(p/.15f,(1-p)/.30f)));
    }
    public int noteVariant() { return noteVariant; }
    public float noteX() { return x+size*((noteLeft ^ facingLeft)?.01f:.80f); }
    public float noteY() { return y+size*(.52f-.35f*Math.max(0,noteAge)/NOTE_SECONDS); }
    public float noteSize() { return size*.19f; }
    public Pose pose() { return pose; }
    public float x() { return x; }
    public float y() { return y; }
    public float size() { return size; }
    public boolean facingLeft() { return facingLeft; }

    /** Adjust a just-started resident walk so the finite route can close without a jump. */
    void retargetWalk(float tx, float ty) {
        if (pose != Pose.WALK) throw new IllegalStateException("Not walking");
        targetX = clamp(tx, 0, maxX()); targetY = clamp(ty, 0, maxY());
        float distance = (float) Math.hypot(targetX - x, targetY - y);
        inPlaceWalk = distance < .0001f;
        remaining = inPlaceWalk ? 1.2f : Math.max(.35f, distance / speed) + .5f;
        if (Math.abs(targetX - x) > .0001f) facingLeft = targetX < x;
    }

    void constrainWalkTo(float cx, float cy, float radius) {
        float dx = targetX - cx, dy = targetY - cy;
        float distance = (float) Math.hypot(dx, dy);
        if (distance > radius) retargetWalk(cx + dx / distance * radius, cy + dy / distance * radius);
    }
}
