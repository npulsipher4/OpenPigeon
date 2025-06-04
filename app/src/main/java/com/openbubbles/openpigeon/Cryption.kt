package com.openbubbles.openpigeon

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

object Cryption {
    class Rand48(seed: Long) {
        private var n: Long = seed

        fun seed(seed: Long) {
            n = seed
        }

        fun srand(seed: Int) {
            n = ((seed.toLong() shl 16) + 0x330e)
        }

        fun next(): Long {
            n = (25214903917L * n + 11) and (1L shl 48) - 1
            return n
        }

        fun drand(): Double {
            return next().toDouble() / (1L shl 48)
        }

        fun lrand(): Int {
            return (next() shr 17).toInt()
        }

        fun mrand(): Int {
            var num = (next() shr 16).toInt()
            if (num and (1 shl 31) != 0) {
                num -= (1 shl 32)
            }
            return num
        }
    }

    fun decrypt(string: String): String {
        val rand = Rand48(0)
        rand.srand(string.length * 0xef)

        val offsets = mutableListOf<Int>()
        var modifier = 0

        for (char in string) {
            offsets.add(floor(rand.drand() * (modifier + string.length)).toInt())
            modifier--
        }

        var output = ""
        for ((i, offset) in offsets.reversed().withIndex()) {
            val index = string.length - i - 1
            output = output.substring(0, offset) + string[index] + output.substring(offset)
        }

        return output
    }

    fun encrypt(string: String): String {
        val rand = Rand48(0)
        rand.srand(string.length * 0xef)

        var result = ""
        var remaining = string

        for (i in 0 until string.length) {
            val idx = floor(rand.drand() * remaining.length).toInt()
            result += remaining[idx]
            remaining = remaining.substring(0, idx) + remaining.substring(idx + 1)
        }

        return result
    }


    fun getId(): String {
        val randBytes = ByteArray(12)
        Random.nextBytes(randBytes)
        val id = Base64.encodeToString(randBytes, Base64.DEFAULT)
        return id
    }

    fun getAvatarString(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("openpigeon", Context.MODE_PRIVATE)
        val avatarString: String? = sharedPrefs.getString("avatar_string", null)
        if (avatarString.isNullOrEmpty()) {
            val defaultAvatar = "body,0|eyes,0|mouth,0|acc,0|wins,0|bg_color,0.313979,0.237515,0.422361|body_color,0.992157,0.690196,0.435294|glasses,0|stache,0|backdrop,0|hair,4|clothes,1|hair_color,0.345098,0.180392,0.125490|clothes_color,0.706423,0.593545,0.596056" // TODO() Add default avatar string
            sharedPrefs.edit { putString("avatar_string", defaultAvatar) }
            return defaultAvatar
        }
        return avatarString
    }

    fun getAvatar(context: Context): Drawable {
        val avatarData = getAvatarString(context).split("|").associate { entry ->
            val parts = entry.split(",")
            val key = parts[0]
            val values = parts.drop(1)

            key to when (values.size) {
                1 -> values[0].toInt()
                3 -> Triple(values[0].toFloat(), values[1].toFloat(), values[2].toFloat())
                else -> values // fallback if some field is unexpected
            }
        }

        val drawables = mutableListOf<Drawable>()

        // Body
        val bodyDrawable = AvatarPathDrawable().apply {
            setPathData(context.getString(R.string.avatar_body_path))
            setColor(android.graphics.Color.rgb(
                (avatarData["body_color"] as Triple<*, *, *>).first as Float * 255,
                (avatarData["body_color"] as Triple<*, *, *>).second as Float * 255,
                (avatarData["body_color"] as Triple<*, *, *>).third as Float * 255
            ))
        }
        drawables.add(bodyDrawable)

        // Clothes
        val clothesDrawable = AvatarPathDrawable().apply {
            setPathData(context.getString(R.string.avatar_clothes_path))
            setColor(android.graphics.Color.rgb(
                (avatarData["clothes_color"] as Triple<*, *, *>).first as Float * 255,
                (avatarData["clothes_color"] as Triple<*, *, *>).second as Float * 255,
                (avatarData["clothes_color"] as Triple<*, *, *>).third as Float * 255
            ))
        }
        drawables.add(clothesDrawable)

        // Eyes
        val eyesDrawable = AvatarPathDrawable().apply {
            setPathData(context.getString(R.string.avatar_eyes_path))
            setColor(Color.BLACK)
            setAlpha((0.7 * 255).toInt())
        }
        drawables.add(eyesDrawable)

        // Mouth
        val mouthDrawable = AvatarPathDrawable().apply {
            setPathData(context.getString(R.string.avatar_mouth_path))
            setColor(Color.BLACK)
            setAlpha((0.6 * 255).toInt())
        }
        drawables.add(mouthDrawable)

        // Hair
        val hairDrawable = AvatarPathDrawable().apply {
            setPathData(context.getString(R.string.avatar_hair_path))
            setColor(android.graphics.Color.rgb(
                (avatarData["hair_color"] as Triple<*, *, *>).first as Float * 255,
                (avatarData["hair_color"] as Triple<*, *, *>).second as Float * 255,
                (avatarData["hair_color"] as Triple<*, *, *>).third as Float * 255
            ))
        }
        drawables.add(hairDrawable)

        return LayerDrawable(drawables.toTypedArray())
    }


    private const val PREFIX: String = "data:?ver=52&data="
}