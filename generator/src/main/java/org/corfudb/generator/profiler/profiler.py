import matplotlib.pyplot as plt
import numpy as np
import os as os

#input_path = "/Users/vshriya/Desktop/current/CorfuDB/test/client-1529017385323.log"
input_path = "/Users/vshriya/Documents/CorfuDB/CorfuDB/generator/src/main/java/org/corfudb/generator/profiler/data/client-1524272351029.log"
input_directory = "/Users/vshriya/Desktop/current/CorfuDB/test/my_files"
output_path = "results/shriya/new/"
points = []  # all data points, aka each entry in log files
all_ops = []  # all operations mentioned in log files


### Helper Functions
def setup():
    global points, all_ops

    # open file and populate global vars
    f = open(input_path)
    for point in f.readlines():
        points.append(point)
        if get_event_name(point) not in all_ops:
            all_ops.append(get_event_name(point))

    # sort points by start time
    points.sort(key=lambda x: get_time_stamp(x))

def setup_all():
    global points, all_ops

    # open file and populate global vars
    for subdir, dirs, files in os.walk(input_directory):
        for file in files:
            # print os.path.join(subdir, file)
            filepath = subdir + os.sep + file
            print(filepath)

            f = open(filepath)
            for point in f.readlines():
                points.append(point)
                if get_event_name(point) not in all_ops:
                    all_ops.append(get_event_name(point))

    # sort points by start time
    print(len(points))
    points.sort(key=lambda x: get_time_stamp(x))


def get_time_stamp(entry):
    return int(entry.split()[0])


def get_thread_ID(entry):
    return int(entry.split()[1])


def get_event_name(entry):
    return entry.split()[3].split()[0]


def get_duration(entry):
    if "[dur]" not in entry:
        return 0
    return int(entry.split("[dur]")[1].split()[0])


def get_method_name(entry):
    return entry.split("[method]")[1].split()[0]


def nano_to_milli(time):
    return time / 1000000.0


### Analysis
def count_active_threads():
    '''
    Answers the query: how many threads are active per millisecond?
    Creates a scatter plot
    '''
    print 1
    # Count active threads per ms
    counts = {}  # ms interval --> set of active thread IDs
    for point in points:
        if int(nano_to_milli(get_time_stamp(point))) in counts:
            counts[int(nano_to_milli(get_time_stamp(point)))].add(get_thread_ID(point))
        else:
            counts[int(nano_to_milli(get_time_stamp(point)))] = set([get_thread_ID(point)])

    # Create scatter plot
    x = []
    y = []
    for key in counts.keys():
        x += [key - min(counts.keys())]
        y += [len(counts[key])]

    plt.scatter(x, y)
    plt.title("Number of Active Threads vs. Time Elapsed")
    plt.xlabel("Time Elapsed (ms)")
    plt.ylabel("Number of Active Threads")
    plt.savefig(output_path + "count_active_threads.png", bbox_inches='tight')
    plt.clf()


def count_seq_calls():
    '''
    Answers the query: how many sequencer calls are made per millisecond?
    Creates a scatter plot
    '''
    print 2
    # Count sequencer calls per ms
    counts = {}  # ms interval --> counts of seq calls
    for point in points:
        if get_event_name(point) == "Seq":
            if int(nano_to_milli(get_time_stamp(point))) in counts:
                counts[int(nano_to_milli(get_time_stamp(point)))] += 1
            else:
                counts[int(nano_to_milli(get_time_stamp(point)))] = 1

    # Create scatter plot
    x = []
    y = []
    for key in counts.keys():
        x += [key - min(counts.keys())]
        y += [counts[key]]

    plt.scatter(x, y)
    plt.title("Number of Seq Calls vs. Time Elapsed")
    plt.xlabel("Time Elapsed (ms)")
    plt.ylabel("Number of Seq Calls")
    plt.savefig(output_path + "count_seq_calls.png", bbox_inches='tight')
    plt.clf()


def count_ops_per_tx():
    '''
    Answers the query: how many operations are performed per transaction?
    Creates a histogram
    '''
    print 3
    # Count number of ops in every transaction
    counts = {}  # thread num --> count of ops (in one tx in that thread)
    num_ops = []  # list of number of ops taken by each tx
    for point in points:
        if get_event_name(point) == "TXBegin":
            counts[get_thread_ID(point)] = 0
        elif get_event_name(point) == "TXEnd":
            num_ops += [counts[get_thread_ID(point)]]
            counts[get_thread_ID(point)] = 0
        elif get_thread_ID(point) in counts:
            counts[get_thread_ID(point)] += 1

    # Count frequency of each number of ops
    num_num_ops = {}  # number --> number of times that number appears
    for n in num_ops:
        if n not in num_num_ops:
            num_num_ops[n] = 1
        else:
            num_num_ops[n] += 1
    x = num_num_ops.keys()
    y = num_num_ops.values()

    # Create histogram
    data = []
    for num in num_num_ops:
        for i in range(num_num_ops[num]):
            data += [num]
    weights = np.ones_like(data) / float(len(data))
    plt.hist(data, weights=weights, facecolor='limegreen')

    plt.xlabel('Num Ops')
    plt.ylabel('Frequency')
    plt.title('Num Ops per Transaction')
    plt.grid(True)
    plt.savefig(output_path + "count_ops_per_tx.png", bbox_inches='tight')
    plt.clf()


def count_table_ops():
    '''
    Answers query: how many read/write table operations are performed on each table?
    Creates a horizontal stacked bar chart
    '''
    print 3.5

    counts = {} # table_id --> {op --> count}
    for point in points:
        if "[id]" in point or "[ids]" in points:
            if get_table_ID(point) not in counts:
                counts[get_table_ID(point)] = {}
            if get_event_name(point) not in counts[get_table_ID(point)]:
                counts[get_table_ID(point)][get_event_name(point)] = 1
            else:
                counts[get_table_ID(point)][get_event_name(point)] += 1

    # WIP!!!!!

    num_set = [{'Language': 75, 'Science': 88, 'Math': 96},
               {'Language': 71, 'Science': 95, 'Math': 92},
               {'Language': 75, 'Science': 90, 'Math': 89}]

    lan_guage = [['Language', 'Science', 'Math'],
                 ['Science', 'Math', 'Language'],
                 ['Math', 'Language', 'Science']]
    colors = ["r", "g", "b"]
    names = sorted(num_set[0].keys())
    values = np.array([[data[name] for name in order] for data, order in zip(num_set, lan_guage)])
    lefts = np.insert(np.cumsum(values, axis=1), 0, 0, axis=1)[:, :-1]
    orders = np.array(lan_guage)
    bottoms = np.arange(len(lan_guage))

    for name, color in zip(names, colors):
        idx = np.where(orders == name)
        value = values[idx]
        left = lefts[idx]
        plt.bar(left=left, height=0.8, width=value, bottom=bottoms,
                color=color, orientation="horizontal", label=name)
    plt.yticks(bottoms + 0.4, ["Student-%d" % (t + 1) for t in bottoms])
    plt.legend(loc="best", bbox_to_anchor=(1.0, 1.00))
    plt.subplots_adjust(right=0.75)
    # Turn on the grid
    plt.minorticks_on()
    plt.grid(which='major', linestyle='-', linewidth='0.5', color='green')
    plt.grid(which='minor', linestyle=':', linewidth='0.5', color='black')


def count_table_ops_per_tx(access_ops, mutate_ops):  # access_ops, mutate_ops = list of strings with names of ops
    '''
    Answers query: how many read/write table operations are performed per transaction?
    Creates a double bar graph
    '''
    print 4
    counts_access = {}  # thread num --> count of access ops
    counts_mutate = {}  # thread num --> count of mutate ops
    num_access_ops = []  # contains total number of access ops in each tx
    num_mutate_ops = []  # contains total number of mutate ops in each tx
    for point in points:
        if get_event_name(point) == "TXBegin":
            counts_access[get_thread_ID(point)] = 0
            counts_mutate[get_thread_ID(point)] = 0
        elif get_event_name(point) == "TXEnd":
            num_access_ops += [counts_access[get_thread_ID(point)]]
            num_mutate_ops += [counts_mutate[get_thread_ID(point)]]
            counts_access[get_thread_ID(point)] = 0
            counts_mutate[get_thread_ID(point)] = 0
        else:
            for op in access_ops:
                if "[method] " + op in point and get_thread_ID(point) in counts_access:
                    counts_access[get_thread_ID(point)] += 1
                    break
            for op in mutate_ops:
                if "[method] " + op in point and get_thread_ID(point) in counts_mutate:
                    counts_mutate[get_thread_ID(point)] += 1
                    break

    # Count frequency of each number of ops
    num_num_access_ops = {}  # number --> number of times that number appears
    for n in num_access_ops:
        if n not in num_num_access_ops:
            num_num_access_ops[n] = 1
        else:
            num_num_access_ops[n] += 1

    num_num_mutate_ops = {}  # number --> number of times that number appears
    for n in num_mutate_ops:
        if n not in num_num_mutate_ops:
            num_num_mutate_ops[n] = 1
        else:
            num_num_mutate_ops[n] += 1

    # Create double bar chart
    x = np.array(list(set(num_num_access_ops.keys()).union(set(num_num_mutate_ops.keys()))))
    y_access = []
    y_mutate = []
    for item in x:
        if item not in num_num_access_ops:
            num_num_access_ops[item] = 0
        if item not in num_num_mutate_ops:
            num_num_mutate_ops[item] = 0
        y_access += [num_num_access_ops[item]]
        y_mutate += [num_num_mutate_ops[item]]
    width = 0.27
    plt.bar(x, y_access, width, color="blue", label="accessOps")
    plt.bar(x + width, y_mutate, width, color="red", label="mutateOps")
    plt.legend(loc=1)
    plt.title("Num Ops per Transaction")
    plt.xlabel("Num Ops")
    plt.ylabel("Num Txs")
    plt.savefig(output_path + "count_table_ops_per_tx.png", bbox_inches='tight')
    plt.clf()


def count_id_table_ops_per_tx(access_ops, mutate_ops,table_id):
    '''
    Answers query: how many read/write table operations are performed on a given table per transaction?
    Creates a double bar graph
    '''
    print 5
    counts_access = {}  # thread num --> count of access ops
    counts_mutate = {}  # thread num --> count of mutate ops
    num_access_ops = []  # contains total number of access ops in each tx
    num_mutate_ops = []  # contains total number of mutate ops in each tx
    for point in points:
        if get_event_name(point) == "TXBegin":
            counts_access[get_thread_ID(point)] = 0
            counts_mutate[get_thread_ID(point)] = 0
        elif get_event_name(point) == "TXEnd":
            num_access_ops += [counts_access[get_thread_ID(point)]]
            num_mutate_ops += [counts_mutate[get_thread_ID(point)]]
            counts_access[get_thread_ID(point)] = 0
            counts_mutate[get_thread_ID(point)] = 0
        elif table_id in point:
            for op in access_ops:
                if "[method] " + op in point and get_thread_ID(point) in counts_access:
                    counts_access[get_thread_ID(point)] += 1
                    break
            for op in mutate_ops:
                if "[method] " + op in point and get_thread_ID(point) in counts_mutate:
                    counts_mutate[get_thread_ID(point)] += 1
                    break

    # Count frequency of each number of ops
    num_num_access_ops = {}  # number --> number of times that number appears
    for n in num_access_ops:
        if n not in num_num_access_ops:
            num_num_access_ops[n] = 1
        else:
            num_num_access_ops[n] += 1

    num_num_mutate_ops = {}  # number --> number of times that number appears
    for n in num_mutate_ops:
        if n not in num_num_mutate_ops:
            num_num_mutate_ops[n] = 1
        else:
            num_num_mutate_ops[n] += 1

    # Create double bar chart
    x = np.array(list(set(num_num_access_ops.keys()).union(set(num_num_mutate_ops.keys()))))
    y_access = []
    y_mutate = []
    for item in x:
        if item not in num_num_access_ops:
            num_num_access_ops[item] = 0
        if item not in num_num_mutate_ops:
            num_num_mutate_ops[item] = 0
        y_access += [num_num_access_ops[item]]
        y_mutate += [num_num_mutate_ops[item]]
    width = 0.27
    plt.bar(x, y_access, width, color="blue", label="accessOps")
    plt.bar(x + width, y_mutate, width, color="red", label="mutateOps")
    plt.legend(loc=1)
    plt.title("Num Ops per Transaction")
    plt.xlabel("Num Ops")
    plt.ylabel("Num Txs")
    plt.savefig(output_path + "count_id_table_ops_per_tx.png", bbox_inches='tight')
    plt.clf()


def count_all_ops():
    '''
    Answers the query: how many times was each operation called w.r.t. the total number of calls?
    Creates a pie chart
    '''
    print 6
    # Count number of times each operation was called
    counts = {}  # op name --> count
    for point in points:
        if get_event_name(point) not in counts:
            counts[get_event_name(point)] = 1
        else:
            counts[get_event_name(point)] += 1

    # Sort values and labels in ascending order by value
    values = []
    labels = []
    other = 0
    total_count = sum(counts.values())
    for key, value in sorted(counts.iteritems(), key=lambda (k, v): (v, k)):
        if value / (1.0 * total_count) < 0.015:
            other += value
            continue
        labels += [key + "\n(" + str(value) + ")"]  # label = name of op + num of occurrences [string]
        values += [value]  # value = time taken by op
    labels += ["Other" + "\n(" + str(other) + ")"]
    values += [other]

    # Create pie chart
    colors = plt.cm.Set2(np.array([(i - len(counts) / 16) / (len(counts) * 2.0) for i in range(0, 2 * len(counts), 2)]))

    fig = plt.figure(figsize=[10, 10])
    ax = fig.add_subplot(111)
    pie_wedge_collection = ax.pie(values, colors=colors, labels=labels, pctdistance=0.7, radius=3, autopct='%1.1f%%')
    plt.axis('equal')
    for pie_wedge in pie_wedge_collection[0]:
        pie_wedge.set_edgecolor('white')
    fig.text(.5, .05, "Total Count: " + str(total_count) + " ops", ha='center', fontweight="bold")

    plt.savefig(output_path + "count_all_ops.png", bbox_inches='tight')
    plt.clf()


def count_all_methods():
    '''
    Answers the query: how many times was each method called w.r.t. the total number of calls?
    Creates a pie chart
    '''
    print 7
    # Count number of times each operation was called
    counts = {}  # op name --> count
    for point in points:
        if "[method]" in point:
            if get_method_name(point) not in counts:
                counts[get_method_name(point)] = 1
            else:
                counts[get_method_name(point)] += 1

    # Sort values and labels in ascending order by value
    values = []
    labels = []
    other = 0
    total_count = sum(counts.values())
    for key, value in sorted(counts.iteritems(), key=lambda (k, v): (v, k)):
        if value / (1.0 * total_count) < 0.015:
            print(value)
            other += value
            continue
        labels += [key + "\n(" + str(value) + ")"]  # label = name of op + num of occurrences [string]
        values += [value]  # value = time taken by op
    labels += ["Other" + "\n(" + str(other) + ")"]
    values += [other]

    # Create pie chart
    colors = plt.cm.Set2(np.array([(i - len(counts) / 16) / (len(counts) * 2.0) for i in range(0, 2 * len(counts), 2)]))

    fig = plt.figure(figsize=[10, 10])
    ax = fig.add_subplot(111)
    pie_wedge_collection = ax.pie(values, colors=colors, labels=labels, pctdistance=0.7, radius=3, autopct='%1.1f%%')
    plt.axis('equal')
    for pie_wedge in pie_wedge_collection[0]:
        pie_wedge.set_edgecolor('white')
    fig.text(.5, .05, "Total Count: " + str(total_count) + " ops", ha='center', fontweight="bold")

    plt.savefig(output_path + "count_all_methods.png", bbox_inches='tight')
    plt.clf()


def count_all_methods_tx():
    '''
    Answers the query: how many times was each method called in a transaction w.r.t. the total number of calls?
    Creates a pie chart
    '''
    print 8
    # Count number of times each operation was called
    counts = {}  # op name --> count
    for point in points:
        if "(tx)" in point and "[method]" in point:
            if get_method_name(point) not in counts:
                counts[get_method_name(point)] = 1
            else:
                counts[get_method_name(point)] += 1

    # Sort values and labels in ascending order by value
    values = []
    labels = []
    other = 0
    total_count = sum(counts.values())
    for key, value in sorted(counts.iteritems(), key=lambda (k, v): (v, k)):
        if value / (1.0 * total_count) < 0.015:
            print(value)
            other += value
            continue
        labels += [key + "\n(" + str(value) + ")"]  # label = name of op + num of occurrences [string]
        values += [value]  # value = time taken by op
    labels += ["Other" + "\n(" + str(other) + ")"]
    values += [other]

    # Create pie chart
    colors = plt.cm.Set2(np.array([(i - len(counts) / 16) / (len(counts) * 2.0) for i in range(0, 2 * len(counts), 2)]))

    fig = plt.figure(figsize=[10, 10])
    ax = fig.add_subplot(111)
    pie_wedge_collection = ax.pie(values, colors=colors, labels=labels, pctdistance=0.7, radius=3, autopct='%1.1f%%')
    plt.axis('equal')
    for pie_wedge in pie_wedge_collection[0]:
        pie_wedge.set_edgecolor('white')
    fig.text(.5, .05, "Total Count: " + str(total_count) + " ops", ha='center', fontweight="bold")

    plt.savefig(output_path + "count_all_methods_tx.png", bbox_inches='tight')
    plt.clf()


def count_all_methods_by_id(table_id):
    '''
    Answers the query: how many times was each method called for a given table w.r.t. the total number of calls?
    Creates a pie chart
    '''
    print 9
    # Count number of times each operation was called
    counts = {}  # op name --> count
    for point in points:
        if table_id in point and "[method]" in point:
            if get_method_name(point) not in counts:
                counts[get_method_name(point)] = 1
            else:
                counts[get_method_name(point)] += 1

    # Sort values and labels in ascending order by value
    values = []
    labels = []
    other = 0
    total_count = sum(counts.values())
    for key, value in sorted(counts.iteritems(), key=lambda (k, v): (v, k)):
        if value / (1.0 * total_count) < 0.015:
            print(value)
            other += value
            continue
        labels += [key + "\n(" + str(value) + ")"]  # label = name of op + num of occurrences [string]
        values += [value]  # value = time taken by op
    labels += ["Other" + "\n(" + str(other) + ")"]
    values += [other]

    # Create pie chart
    colors = plt.cm.Set2(np.array([(i - len(counts) / 16) / (len(counts) * 2.0) for i in range(0, 2 * len(counts), 2)]))

    fig = plt.figure(figsize=[10, 10])
    ax = fig.add_subplot(111)
    pie_wedge_collection = ax.pie(values, colors=colors, labels=labels, pctdistance=0.7, radius=3, autopct='%1.1f%%')
    plt.axis('equal')
    for pie_wedge in pie_wedge_collection[0]:
        pie_wedge.set_edgecolor('white')
    fig.text(.5, .05, "Total Count: " + str(total_count) + " ops", ha='center', fontweight="bold")

    plt.savefig(output_path + "count_all_methods_by_id.png", bbox_inches='tight')
    plt.clf()


def count_all_ops_time():
    '''
    Answers the query: how much time did a single call of each operation take on average?
    Creates a box plot and a bar chart
    '''
    print 10
    # Count total time taken by each op
    counts = {}  # op name --> list of durations (ms)
    for point in points:
        if get_event_name(point) not in counts:
            counts[get_event_name(point)] = [nano_to_milli(get_duration(point))]
        else:
            counts[get_event_name(point)] += [nano_to_milli(get_duration(point))]

    # Create box plot with entire distribution
    fig = plt.figure()
    plt.hold = True
    boxes = []
    # for op in counts.keys():
    #     boxes.append([counts[op]])
    # plt.boxplot(boxes, vert=0, sym='')
    yticks = []
    for key, value in sorted(counts.iteritems(), key=lambda (k, v): -1 * np.mean(v)):
        boxes.append([counts[key]])
        yticks.append(key)
    plt.boxplot(boxes, vert=0, sym='')

    means = [np.mean(x) for x in boxes]
    plt.scatter(means, np.arange(1, len(counts.keys()) + 1))
    plt.yticks(np.arange(1, len(counts.keys()) + 1), yticks)
    plt.xlim(left=-1)
    plt.xlabel("Time (ms)")
    plt.title("Time per Operation")

    plt.savefig(output_path + "count_all_ops_time_boxplot.png", bbox_inches='tight')
    plt.clf()

    # Average out time for each op
    total_time = 0
    for op in counts:  # counts: op name --> avg time taken per call of op
        counts[op] = sum(counts[op]) / (len(counts[op]) * 1.0)
        total_time += counts[op]

    # Sort values and labels in ascending order by value
    values = []
    labels = []
    for key, value in sorted(counts.iteritems(), key=lambda (k, v): (v, k)):
        labels += [key + "\n" + str(value) + " ms"]  # label = name of op + time taken by op (ms) [string]
        values += [value]  # value = time taken by op

    # Color Scheme
    colors = plt.cm.Set2(np.array([(i - len(labels) / 16) / (len(labels) * 2.0) for i in range(0, 2*len(labels), 2)]))

    # Create bar graph
    plt.rcdefaults()
    fig, ax = plt.subplots()

    labels = [l.split("\n")[0] for l in labels]
    y_pos = np.arange(len(labels))

    r = ax.barh(y_pos, values, align='center', color=colors, ecolor='black')
    ax.set_yticks(y_pos)
    ax.set_yticklabels(labels)
    ax.invert_yaxis()  # labels read top-to-bottom
    ax.set_xlabel('Time (ms)')
    ax.set_title('Average Time per Operation')

    for rect in r:
        width = rect.get_width()
        plt.text(rect.get_width() + 2, rect.get_y() + 0.5 * rect.get_height(),
                 '%f' % width, ha='center', va='center')

    plt.savefig(output_path + "count_all_ops_time_bar.png", bbox_inches='tight')
    plt.clf()


def count_all_ops_time_by_tx():
    '''
    Answers the query: how much time did each transactional operation take on average across all transactions
    w.r.t. the total runtime of all transactions?
    Creates a pie chart and two bar charts
    '''
    print 11
    # Count total time taken by each op w/i each tx
    # NOT CORRECT for same reason as above!
    txs = []  # list of lists, txs[i] = np.array of how much time each op takes in tx i
    in_tx = {}  # thread # --> (bool) currently in tx
    counts = {}  # thread # --> {op name --> list of durations (ms)}
    for point in points:
        if get_event_name(point) == "TXBegin":
            in_tx[get_thread_ID(point)] = True

        # only consider ops that are part of a tx
        if get_thread_ID(point) in in_tx and in_tx[get_thread_ID(point)]:
            if get_thread_ID(point) not in counts:
                counts[get_thread_ID(point)] = {}

            if get_event_name(point) not in counts[get_thread_ID(point)]:
                counts[get_thread_ID(point)][get_event_name(point)] = [nano_to_milli(get_duration(point))]
            else:
                counts[get_thread_ID(point)][get_event_name(point)] += [nano_to_milli(get_duration(point))]

        if get_event_name(point) == "TXEnd":
            # append info in counts to txs
            in_tx[get_thread_ID(point)] = False
            tx = []
            for i in range(len(all_ops)):
                if all_ops[i] in counts[get_thread_ID(point)]:
                    tx[i] = counts[get_thread_ID(point)][all_ops[i]]
                else:
                    tx[i] = []
            txs += [tx]
            # clear time_count for that thread
            del counts[get_thread_ID(point)]

    tx_mtx = np.array([tx for tx in txs])
    tx_mtx_T = tx_mtx.T  # aggregates counts s.t. each op has its own list

    avg_vals = []  # list of avg time taken by each op
    for op in tx_mtx_T:
        avg_vals += [np.average(op)]

    # create pie chart
    plt.pie(avg_vals, labels=all_ops, colors=['gold', 'yellowgreen', 'lightcoral', 'lightskyblue'], autopct='%1.1f%%')
    plt.axis('equal')
    plt.savefig(output_path + "count_all_ops_time_by_tx.png", bbox_inches='tight')
    plt.clf()


def count_reads():
    '''
    Answers the query: how many reads (both tx and non-tx) are done per millisecond?
    Creates a scatter plot, with both tx reads and non-tx reads
    '''
    print 12
    # Count tx reads
    counts_tx = {}  # ms interval --> count of tx reads
    for point in points:
        if get_event_name(point) == "access(tx)":
            if int(nano_to_milli(get_time_stamp(point))) in counts_tx:
                counts_tx[int(nano_to_milli(get_time_stamp(point)))] += 1
            else:
                counts_tx[int(nano_to_milli(get_time_stamp(point)))] = 1

    # Count non-tx reads
    counts_non_tx = {}  # ms interval --> count of non-tx reads
    for point in points:
        if get_event_name(point) == "access(nonTx)":
            if int(nano_to_milli(get_time_stamp(point))) in counts_non_tx:
                counts_non_tx[int(nano_to_milli(get_time_stamp(point)))] += 1
            else:
                counts_non_tx[int(nano_to_milli(get_time_stamp(point)))] = 1

    # Create juxtaposed scatter plot
    x_tx = []
    y_tx = []
    for key in counts_tx.keys():
        x_tx += [key - min(counts_tx.keys())]
        y_tx += [counts_tx[key]]
    plt.scatter(x_tx, y_tx, color='green', label="tx reads")

    x_non_tx = []
    y_non_tx = []
    for key in counts_non_tx.keys():
        x_non_tx += [key - min(counts_non_tx.keys())]
        y_non_tx += [counts_non_tx[key]]
    plt.scatter(x_non_tx, y_non_tx, label="non-tx reads")

    plt.title("Tx and Non-Tx Reads vs. Time")
    plt.xlabel("Time Elapsed (ms)")
    plt.ylabel("Number of Reads")
    plt.legend(loc=1)
    plt.savefig(output_path + "count_reads.png", bbox_inches='tight')
    plt.clf()


### Display Results
#setup_all()
setup()
# count_active_threads()
# count_seq_calls()
# count_ops_per_tx()
# count_table_ops()
# count_table_ops_per_tx(["containsKey"], ["put"])
# count_id_table_ops_per_tx(["containsKey"], ["put"], "7c4f2940-7893-3334-a6cb-7a87bf045c0d")
# count_all_ops()
# count_all_methods()
# count_all_methods_tx()
# count_all_methods_by_id("7c4f2940-7893-3334-a6cb-7a87bf045c0d")
count_all_ops_time()
# count_all_ops_time_by_tx()
# count_reads()