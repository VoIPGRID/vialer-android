package com.voipgrid.contacts

import android.content.Context
import android.graphics.*
import com.github.tamir7.contacts.Contact


class PhoneNumberImageGenerator(private val contacts : Contacts, private val context: Context) {

    /**
     * Perform a look-up in the contacts to find a contact image, if one is not found then generate
     * an image based on the caller's contact name.
     *
     */
    fun find(number : String): Bitmap? {
        val contact : Contact? = contacts.getContactByPhoneNumber(number)

        if (contact == null) {
            return IconHelper.getCallerIconBitmap(context, "", number, 0)
        }

        val contactImage = contacts.getContactImageByPhoneNumber(number)

        return contactImage ?: IconHelper.getCallerIconBitmap(context, contact.displayName.substring(0, 1), number, 0)
    }

    /**
     * Automatically round any corners that are found when searching for an image.
     * @see {find}
     *
     */
    fun findWithRoundedCorners(number : String) : Bitmap? {
        return roundCorners(find(number))
    }

    private fun roundCorners(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) {
            return null
        }

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)

        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = bitmap.width.toFloat()

        paint.setAntiAlias(true)
        canvas.drawARGB(0, 0, 0, 0)
        paint.setColor(color)
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

}