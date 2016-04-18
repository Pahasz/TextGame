package com.quickbite.rx2020

import com.badlogic.gdx.math.MathUtils
import com.quickbite.rx2020.managers.EventManager
import com.quickbite.rx2020.managers.GroupManager

/**
 * Created by Paha on 2/8/2016.
 */
class Person(private val _firstName:String, private val _lastName:String) {
    var firstName:String = ""
        get() = _firstName
    var lastName:String = ""
        get() = _lastName
    var fullName:String = ""
        get() = "$_firstName $_lastName"

    var healthNormal:Float = 100f
        get
        private set

    var healthInjury:Float = 0f
        get
        private set

    var maxHealth = 100f
        get
        private set

    var numInjury = 0
    var numSickness = 0


    private var disabilities:MutableList<Disability> = mutableListOf()
    var disabilityList:List<Disability>
        get() = disabilities.toList()
        set(value){
            disabilities.clear()
            value.forEach { injury -> addDisability(injury) }
        }

    val hasInjury:Boolean
        get() = numInjury > 0

    val hasSickness:Boolean
        get() = numSickness > 0

    constructor(name:Pair<String, String>):this(name.first, name.second)

    constructor(name:Pair<String, String>, maxHealth:Float):this(name.first, name.second){
        this.maxHealth = maxHealth
        this.healthNormal = maxHealth
    }

    operator fun component1() = _firstName
    operator fun component2() = _lastName

    fun addHealth(amt:Float):Float{
        healthNormal +=amt
        if(healthNormal >= maxHealth - healthInjury)
            healthNormal = maxHealth - healthInjury
        if(healthNormal <= 0)
            GroupManager.killPerson(firstName)

        EventManager.callEvent("healthChanged", this, amt)
        return amt
    }

    fun addPercentHealth(perc:Float):Float{
        val amt = maxHealth*(perc/100f)

        EventManager.callEvent("healthChanged", this, amt)
        return addHealth(amt)
    }

    fun addDisability(level: Disability.DisabilityLevel, type:Disability.DisabilityType){
        val disability = Disability(level, type)
        disabilities.add(disability)
        val isInjury = disability.type == Disability.DisabilityType.Injury

        //Only shift health if it's an injury
        if(isInjury) {
            healthNormal -= disability.hpLost
            healthInjury += disability.hpLost
        }

        //Increment the right counter
        when(isInjury){
            true -> numInjury++
            else -> numSickness++
        }
    }

    fun removeDisability(disability: Disability){
        val removed = disabilities.remove(disability)
        val isInjury = disability.type == Disability.DisabilityType.Injury
        //Only shift health if it's an injury
        if(removed && isInjury) {
            healthNormal += disability.hpLost
            healthInjury -= disability.hpLost
        }

        //Decrement the right counter
        when(isInjury){
            true -> numInjury--
            else -> numSickness--
        }
    }

    fun removeWorstDisability(){
        var worst: Disability? = null
        disabilityList.forEach { injury ->
            if(worst == null || worst!!.type < injury.type)
                worst = injury
        }

        if(worst!=null)
            removeDisability(worst!!)
    }

    fun removeLongestDisability(){
        var longest: Disability? = null
        disabilityList.forEach { injury ->
            if(longest == null || longest!!.hoursRemaining < injury.hoursRemaining)
                longest = injury
        }

        if(longest!=null)
            removeDisability(longest!!)
    }

    /**
     * Used for loading in injuries from a save mostly.
     */
    private fun addDisability(disability: Disability){
        disabilities.add(disability)
        val isInjury = disability.type == Disability.DisabilityType.Injury

        if(isInjury) {
            healthNormal -= disability.hpLost
            healthInjury += disability.hpLost
        }

        when(isInjury){
            true -> numInjury++
            else -> numSickness++
        }
    }

    class Disability(var level: DisabilityLevel, var type:DisabilityType):IUpdateable{
        enum class DisabilityType{Injury, Sickness}
        enum class DisabilityLevel {Minor, Regular, Major, Trauma}
        val done:Boolean
            get() = hoursRemaining <= 0

        //Need this empty constructor for loading/saving to json files.
        private constructor():this(DisabilityLevel.Minor, DisabilityType.Injury)

        var hoursRemaining = 0
        var hpLost = 0
        var hpLostPerHour = 0f

        init{
            when(level){
                DisabilityLevel.Minor ->{ hoursRemaining = MathUtils.random(10*24, 30*24); hpLost = MathUtils.random(0, 25); hpLostPerHour = 0.12f}
                DisabilityLevel.Regular ->{ hoursRemaining = MathUtils.random(30*24, 50*24); hpLost = MathUtils.random(25, 50); hpLostPerHour = 0.14f}
                DisabilityLevel.Major ->{ hoursRemaining = MathUtils.random(50*24, 70*24); hpLost = MathUtils.random(50, 75); hpLostPerHour = 0.19f}
                DisabilityLevel.Trauma ->{ hoursRemaining = MathUtils.random(70*24, 90*24); hpLost = MathUtils.random(75, 100); hpLostPerHour = 0.29f}
            }
        }

        override fun update(delta: Float) {}
        override fun updateHourly(delta: Float) {
            this.hoursRemaining--
        }
    }


    override fun toString(): String {
        return firstName;
    }
}