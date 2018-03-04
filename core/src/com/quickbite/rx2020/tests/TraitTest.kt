package com.quickbite.rx2020.tests

import com.badlogic.gdx.math.MathUtils
import com.quickbite.rx2020.Person
import com.quickbite.rx2020.managers.DataManager
import com.quickbite.rx2020.managers.GroupManager
import com.quickbite.rx2020.managers.TraitManager
import com.quickbite.rx2020.objects.Ailment

object TraitTest {

    private val listOfTraitsAdded = mutableListOf<DataManager.TraitJson>()

    fun testInjuryTraits(){
        val injuryDuration = DataManager.traitList.professions.listOfTraits.first {
            it.effects.firstOrNull { it.affects == "addAilment" && it.subCommand == "duration" && it.scope == "global"} != null }
        val injuryDamage = DataManager.traitList.professions.listOfTraits.first {
            it.effects.firstOrNull { it.affects == "addAilment" && it.subCommand == "damage"&& it.scope == "global" } != null }

        val injuryDurationIndividual = DataManager.traitList.professions.listOfTraits.first {
            it.effects.firstOrNull { it.affects == "addAilment" && it.subCommand == "duration" && it.scope == "individual" } != null }
        val injuryDamageIndividual = DataManager.traitList.professions.listOfTraits.first {
            it.effects.firstOrNull { it.affects == "addAilment" && it.subCommand == "damage" && it.scope == "individual"} != null }

        //Add at least one person to the group manager for testing
        val person = GroupManager.addPerson(Person("Mike", "Thomas", 100f, true, 0L))
        person.addAilment(Ailment.AilmentLevel.Regular, Ailment.AilmentType.Injury)

        System.out.println("Before adding trait... Health: ${person.healthNormal}, Injury health: ${person.healthInjury}, Duration: ${person.ailmentList[0].hoursRemaining}")

        TraitManager.addTrait(injuryDuration)
        TraitManager.addTrait(injuryDamage)

        System.out.println("After adding trait... Health: ${person.healthNormal}, Injury health: ${person.healthInjury}, Duration: ${person.ailmentList[0].hoursRemaining}")

        TraitManager.removeTrait(injuryDuration)
        TraitManager.removeTrait(injuryDamage)

        System.out.println("After removing trait... Health: ${person.healthNormal}, Injury health: ${person.healthInjury}, Duration: ${person.ailmentList[0].hoursRemaining}")
    }

    fun test(){
        System.out.println("-- Testing Traits --")

        for(i in 0.until(100)){
            val randomIndex = MathUtils.random(DataManager.traitList.professions.listOfTraits.size-1)
            val randomTrait = DataManager.traitList.professions.listOfTraits[randomIndex]
            TraitManager.addTrait(randomTrait, Person("Mike", "Thomas", 100f, true, 0L))
            System.out.println("Adding trait $randomTrait for Mike")

            listOfTraitsAdded += randomTrait
        }

        System.out.println("After adding traits")
        printDataFromMaps()

        System.out.println()
        System.out.println("Testing modifier amounts")
        testModifierAmount()
        System.out.println()

        removeTraits()
        System.out.println("After removing traits")
        printDataFromMaps()

        System.out.println("-- Done Testing Traits --")

        //Clear these both to make sure nothing gets stuck in here after testing
        TraitManager.individualTraitMap.clear()
        TraitManager.globalTraitMap.clear()
    }

    private fun printDataFromMaps(){
        System.out.println()

        System.out.println("Individuals")
        TraitManager.individualTraitMap.toList().forEach{ kv ->
            System.out.println("${kv.first}:${kv.second.first}:${kv.second.second}")
        } //Print out

        System.out.println()

        System.out.println("Globals")
        TraitManager.globalTraitMap.toList().forEach { kv1 ->
            System.out.println("${kv1.first}:${kv1.second.first}:${kv1.second.second}")
        }
    }

    private fun testModifierAmount(){
        val itemList = DataManager.getItemList()

        for(i in 1 until 20){
            val randomItem = itemList[MathUtils.random(itemList.size - 1)]
            val baseAmount = MathUtils.random(50, 150).toFloat()
            val result:Pair<Float, Boolean>
            result = if(randomItem.type == "ROVPart")
                TraitManager.getTraitModifier("addRndAmt", subType = randomItem.type)
            else
                TraitManager.getTraitModifier("addRndAmt", randomItem.name)

            val modifiedAmount:Float=
                    if(result.second) //If we are using percent
                        baseAmount + baseAmount*(result.first/100f) //Divide by 100 to get actual percent
                    else
                        baseAmount + result.first //If not using percent, add it straight up

            System.out.println("Item ${randomItem.name}")
            System.out.println("Base amount: $baseAmount, Modified amount: $modifiedAmount, modifier: ${result.first}")
        }
    }

    private fun removeTraits(){
        listOfTraitsAdded.forEach{ trait ->
            TraitManager.removeTrait(trait, Person("Mike", "Thomas", 100f, true, 0L))
            System.out.println("Removing trait $trait for Mike")
        }
    }

}