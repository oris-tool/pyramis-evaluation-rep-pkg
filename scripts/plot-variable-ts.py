import os
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import argparse

def get_curve_label(filename):
    # mapping = {
    #     'simulationCDF.txt': 'Simulation',
    #     'pyramisCDF.txt': 'Pyramis',
    # }
    mapping = {
        'GT.txt': 'Ground truth',
        'pyramisCDF_ts0.4.txt': 'Pyramis - time tick 0.4',
        'pyramisCDF_ts0.2.txt': 'Pyramis - time tick 0.2',
        'pyramisCDF_ts0.1.txt': 'Pyramis - time tick 0.1',
    }
    return mapping.get(filename, filename)

def get_curve_style(filename):
    if 'GT' in filename:
        return {'color': 'black', 'linestyle': '--', 'linewidth': 2}  # nero tratteggiato
    elif 'ts0.1' in filename:
        return {'color': '#0077BB', 'linewidth': 2}     # blu intenso
    elif 'ts0.2' in filename:
        return {'color': '#009988', 'linewidth': 2}     # verde acqua
    elif 'ts0.4' in filename:
        return {'color': '#CC3311', 'linewidth': 2}     # rosso

def load_cdf_data(filepath):
    return pd.read_csv(filepath, delim_whitespace=True, header=None, names=['x', 'y'])

def plot_cdfs_from_folder(folder):
    filenames = [ 'GT.txt', 'pyramisCDF_ts0.4.txt', 'pyramisCDF_ts0.2.txt', 'pyramisCDF_ts0.1.txt' ]
    curves = []

    plt.figure(figsize=(10, 6))
    plt.rcParams.update({'font.size': 22})
    plt.rc('axes', labelsize=22)
    plt.rc('legend', fontsize=20)
    plt.rc('xtick', labelsize=20)
    plt.rc('ytick', labelsize=20)

    global_max_x = 0
    
    # Prima passata: trova il massimo valore di x globale
    for filename in filenames:
        file_path = os.path.join(folder, filename)
        if os.path.isfile(file_path):
            df = load_cdf_data(file_path)
            max_x = df['x'].max()
            global_max_x = max(global_max_x, max_x)

    # Seconda passata: plottaggio
    for filename in filenames:
        file_path = os.path.join(folder, filename)
        if os.path.isfile(file_path):
            df = load_cdf_data(file_path)
            style = get_curve_style(filename)
            curve = plt.plot(df['x'], df['y'],
                           label=get_curve_label(filename), 
                           **style)
            curves.append((curve[0], get_curve_label(filename)))

    plt.xlim(0, global_max_x * 1.05)
    plt.xlabel('time')
    plt.ylabel('response time distribution')
    plt.ylim(0, 1)
    
    handles, labels = zip(*curves)
    plt.legend(handles, labels, loc='lower right')
    plt.grid(True)
    plt.tight_layout()

    plt.savefig(os.path.join(folder, "HCDF_comparison.png"))
    plt.savefig(os.path.join(folder, "HCDF_comparison.pdf"))
    plt.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Plot CDF from four files in the same folder.")
    parser.add_argument("folder", help="path to the folder containing the data files")
    args = parser.parse_args()
    plot_cdfs_from_folder(args.folder)



