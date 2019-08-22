package com.ternaryop.feedly

import java.io.IOException

/**
 * Created by dave on 04/09/17.
 * The feedly token has expired
 */

class TokenExpiredException(message: String) : IOException(message)
