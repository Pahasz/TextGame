package com.quickbite.rx2020.managers

import com.badlogic.gdx.math.MathUtils
import com.quickbite.rx2020.Person
import com.quickbite.rx2020.interfaces.IResetable
import com.quickbite.rx2020.interfaces.IUpdateable
import com.quickbite.rx2020.objects.Ailment
import com.quickbite.rx2020.util.Logger
import com.quickbite.rx2020.util.Tester
import com.quickbite.rx2020.objects.Trait

/**
 * Created by Paha on 2/8/2016.
 */
object GroupManager : IUpdateable, IResetable {
    var initialGroupSize:Int = 0
        get
        private set

    private var list:MutableList<Person> = mutableListOf()

    private val emptyFoodHealthDrain = -0.3f

    val numPeopleAlive:Int
        get() = list.size

    fun init(){
        //TODO Should this logic really be in the init function? Maybe move it to a more main area?
        list.clear()
        val maxHealth = if(Tester.TESTING) 1000000f else 100f
        val range = MathUtils.random(0,4) + 4 //4 - 8

        initialGroupSize = range

        for(i in 0 until range) {
            val triple = DataManager.pullRandomName()
            //Make a person
            val person = Person(triple.first, triple.second, MathUtils.random(1f, maxHealth), triple.third, 0)
            //Get a random profession
            val professions = DataManager.traitList.professions
            val randomProfession = professions.listOfTraits[MathUtils.random(professions.listOfTraits.size-1)]
            person.traitList += randomProfession //Add a random profession
            TraitManager.addTrait(randomProfession, person) //Add it into the trait manager
            list.add(person) //Add the person
        }
    }

    override fun update(delta: Float) {

    }

    override fun updateHourly(delta: Float) {
        val outOfFood = SupplyManager.getSupply("edibles").amt <= 0

        //For each person...
        getPeopleList().forEach { person ->

            //Update their injuries. If they are done, remove them from the person.
            person.ailmentList.forEach { disability ->
                disability.updateHourly(delta)
                //If the disability is a sickness, remove health.
                if(disability.type ==  Ailment.AilmentType.Sickness)
                    person.addHealth(-disability.hpLostPerHour)
                //If it's done, remove the disability
                if(disability.done)
                    person.removeAilment(disability)
            }

            //If we are out of food, drain the person's health
            if(outOfFood)
                person.addHealth(emptyFoodHealthDrain)
        }
    }

    /**
     * Returns a list of people (makes a copy of the list)
     */
    fun getPeopleList():Array<Person> = list.toTypedArray()

    fun getPerson(name:String): Person?{
        val person = list.find {person -> person.firstName == name }
        if(person == null) Logger.log("GroupManager", "Trying to find person with name $name and it doesn't exist. People: $list")
        return person
    }

    fun getRandomPerson():Person?{
        if(list.size > 0) {
            return list[MathUtils.random(0, list.size - 1)]
        }else
            return null
    }

    fun clearPeople() = list.clear()

    fun addPerson(person:Person):Person {
        list.add(person)
        return person
    }

    fun killPerson(name:String){
        val person = getPerson(name)
        if(person!=null) {
            list.remove(person)
            EventManager.callEvent("death", listOf(person))
        }
    }

    override fun reset() {
        list = mutableListOf()
    }
}