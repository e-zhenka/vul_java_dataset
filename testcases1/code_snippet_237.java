public static long getNextScheduledTime(final String cronEntry, long currentTime) throws MessageFormatException {

        long result = 0;

        if (cronEntry == null || cronEntry.length() == 0) {
            return result;
        }

        // Handle the once per minute case "* * * * *"
        // starting the next event at the top of the minute.
        if (cronEntry.startsWith("* * * * *")) {
            result = currentTime + 60 * 1000;
            result = result / 1000 * 1000;
            return result;
        }

        List<String> list = tokenize(cronEntry);
        List<CronEntry> entries = buildCronEntries(list);
        Calendar working = Calendar.getInstance();
        working.setTimeInMillis(currentTime);
        working.set(Calendar.SECOND, 0);

        CronEntry minutes = entries.get(MINUTES);
        CronEntry hours = entries.get(HOURS);
        CronEntry dayOfMonth = entries.get(DAY_OF_MONTH);
        CronEntry month = entries.get(MONTH);
        CronEntry dayOfWeek = entries.get(DAY_OF_WEEK);

        // Start at the top of the next minute, cron is only guaranteed to be
        // run on the minute.
        int timeToNextMinute = 60 - working.get(Calendar.SECOND);
        working.add(Calendar.SECOND, timeToNextMinute);

        // If its already to late in the day this will roll us over to tomorrow
        // so we'll need to check again when done updating month and day.
        int currentMinutes = working.get(Calendar.MINUTE);
        if (!isCurrent(minutes, currentMinutes)) {
            int nextMinutes = getNext(minutes, currentMinutes);
            working.add(Calendar.MINUTE, nextMinutes);
        }

        int currentHours = working.get(Calendar.HOUR_OF_DAY);
        if (!isCurrent(hours, currentHours)) {
            int nextHour = getNext(hours, currentHours);
            working.add(Calendar.HOUR_OF_DAY, nextHour);
        }

        // We can roll into the next month here which might violate the cron setting
        // rules so we check once then recheck again after applying the month settings.
        doUpdateCurrentDay(working, dayOfMonth, dayOfWeek);

        // Start by checking if we are in the right month, if not then calculations
        // need to start from the beginning of the month to ensure that we don't end
        // up on the wrong day.  (Can happen when DAY_OF_WEEK is set and current time
        // is ahead of the day of the week to execute on).
        doUpdateCurrentMonth(working, month);

        // Now Check day of week and day of month together since they can be specified
        // together in one entry, if both "day of month" and "day of week" are restricted
        // (not "*"), then either the "day of month" field (3) or the "day of week" field
        // (5) must match the current day or the Calenday must be advanced.
        doUpdateCurrentDay(working, dayOfMonth, dayOfWeek);

        // Now we can chose the correct hour and minute of the day in question.

        currentHours = working.get(Calendar.HOUR_OF_DAY);
        if (!isCurrent(hours, currentHours)) {
            int nextHour = getNext(hours, currentHours);
            working.add(Calendar.HOUR_OF_DAY, nextHour);
        }

        currentMinutes = working.get(Calendar.MINUTE);
        if (!isCurrent(minutes, currentMinutes)) {
            int nextMinutes = getNext(minutes, currentMinutes);
            working.add(Calendar.MINUTE, nextMinutes);
        }

        result = working.getTimeInMillis();

        if (result <= currentTime) {
            throw new ArithmeticException("Unable to compute next scheduled exection time.");
        }

        return result;
    }